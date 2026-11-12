package com.alorma.contactnotes.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PersonDao {
  @Insert
  abstract suspend fun insert(person: PersonEntity): Long

  @Update
  abstract suspend fun update(person: PersonEntity)

  /** Mai sol: [deleteWithTombstones] és l'únic esborrat. */
  @Delete
  protected abstract suspend fun delete(person: PersonEntity)

  /** El `syncId` surt de la fila desada, no de l'entitat que arriba, que podria portar-ne un altre. */
  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) SELECT syncId, :now, 1 FROM person WHERE id = :id",
  )
  protected abstract suspend fun tombstone(
    id: Long,
    now: Long,
  )

  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) " +
      "SELECT syncId, :now, 1 FROM entry WHERE personId = :personId",
  )
  protected abstract suspend fun tombstoneEntriesOf(
    personId: Long,
    now: Long,
  )

  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) " +
      "SELECT syncId, :now, 1 FROM task WHERE personId = :personId",
  )
  protected abstract suspend fun tombstoneTasksOf(
    personId: Long,
    now: Long,
  )

  /**
   * Esborra la persona i en deixa els tombstones: el seu i els de les entrades i tasques que cauen en cascada, que
   * SQLite esborra sense avisar ningú. Els fets fixats viatgen dins de la persona i no en tenen.
   */
  @Transaction
  open suspend fun deleteWithTombstones(
    person: PersonEntity,
    now: Long,
  ) {
    tombstoneEntriesOf(person.id, now)
    tombstoneTasksOf(person.id, now)
    tombstone(person.id, now)
    delete(person)
  }

  @Insert
  protected abstract suspend fun insertFacts(facts: List<PinnedFactEntity>)

  @Query("DELETE FROM pinned_fact WHERE personId = :personId")
  protected abstract suspend fun deleteFacts(personId: Long)

  /** Crea o actualitza la persona amb les seves dades fixades en una sola transacció. La taula `channel` no es toca. */
  @Transaction
  open suspend fun save(
    person: PersonEntity,
    facts: List<PinnedFactEntity>,
  ): Long {
    val id =
      if (person.id == 0L) {
        insert(person)
      } else {
        update(person)
        deleteFacts(person.id)
        person.id
      }
    insertFacts(facts.mapIndexed { index, fact -> fact.copy(id = 0, personId = id, position = index) })
    return id
  }

  // Només per al motor de sincronització: res d'això segella la fila

  @Query("SELECT * FROM person WHERE syncId = :syncId")
  abstract suspend fun getBySyncId(syncId: String): PersonEntity?

  @Query("SELECT * FROM person WHERE dirty = 1")
  abstract suspend fun dirtyRows(): List<PersonEntity>

  @Query("SELECT COUNT(*) FROM person WHERE dirty = 1")
  abstract fun observeDirtyCount(): Flow<Int>

  /** Només si la fila continua tal com es va pujar: una edició feta mentrestant es queda pendent. */
  @Query("UPDATE person SET dirty = 0 WHERE syncId = :syncId AND updatedAt = :modifiedAt")
  abstract suspend fun markClean(
    syncId: String,
    modifiedAt: Long,
  ): Int

  /**
   * Només per al motor: esborra una persona que un altre dispositiu ha esborrat, **sense** tombstone. Amb tombstone,
   * l'esborrat tornaria a pujar. Cau en cascada, com [delete].
   */
  @Query("DELETE FROM person WHERE syncId = :syncId")
  abstract suspend fun deleteFromRemote(syncId: String)

  /**
   * Només per al motor: desa una persona baixada, per `syncId`, **sense segellar**: el `dirty` i la data són els que
   * porta [person]. Conserva l'id local si ja hi era; els fets fixats es substitueixen.
   */
  @Transaction
  open suspend fun applyRemote(
    person: PersonEntity,
    facts: List<PinnedFactEntity>,
  ) {
    val existing = getBySyncId(person.syncId)
    val id =
      if (existing == null) {
        insert(person.copy(id = 0))
      } else {
        update(person.copy(id = existing.id))
        deleteFacts(existing.id)
        existing.id
      }
    insertFacts(facts.mapIndexed { index, fact -> fact.copy(id = 0, personId = id, position = index) })
  }

  @Query("SELECT * FROM person WHERE id = :id")
  abstract fun observe(id: Long): Flow<PersonEntity?>

  @Query("SELECT * FROM person WHERE id = :id")
  abstract suspend fun get(id: Long): PersonEntity?

  @Transaction
  @Query(
    """
    SELECT p.*,
      e.id AS last_id, e.personId AS last_personId, e.kind AS last_kind,
      e.channelType AS last_channelType, e.text AS last_text, e.occurredAt AS last_occurredAt,
      e.customChannelId AS last_customChannelId,
      e.syncId AS last_syncId, e.modifiedAt AS last_modifiedAt, e.dirty AS last_dirty,
      (SELECT COUNT(*) FROM task WHERE personId = p.id AND doneAt IS NULL) AS openTasks
    FROM person p
    LEFT JOIN entry e ON e.id = (
      SELECT id FROM entry WHERE personId = p.id ORDER BY occurredAt DESC, id DESC LIMIT 1
    )
    WHERE p.archived = :archived
    ORDER BY COALESCE(e.occurredAt, p.createdAt) DESC
    """,
  )
  abstract fun observeList(archived: Boolean): Flow<List<PersonListItem>>

  @Query(
    """
    SELECT COUNT(*) FROM person
    WHERE archived = 0 AND id != :excludeId AND alias = :alias COLLATE NOCASE
    """,
  )
  abstract suspend fun countAliasConflicts(
    alias: String,
    excludeId: Long,
  ): Int
}

@Dao
abstract class EntryDao {
  @Insert
  abstract suspend fun insert(entry: EntryEntity): Long

  @Update
  abstract suspend fun update(entry: EntryEntity)

  /** Mai sol: [deleteWithTombstone] és l'únic esborrat d'una entrada. */
  @Delete
  protected abstract suspend fun delete(entry: EntryEntity)

  /** El `syncId` surt de la fila desada, no de l'entitat que arriba. */
  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) SELECT syncId, :now, 1 FROM entry WHERE id = :id",
  )
  protected abstract suspend fun tombstone(
    id: Long,
    now: Long,
  )

  @Query("DELETE FROM sync_tombstone WHERE syncId = :syncId")
  protected abstract suspend fun clearTombstone(syncId: String)

  /**
   * Insereix una entrada que torna a existir: si era un «Desfés» d'un esborrat, el seu tombstone se'n va, perquè una
   * fila no pot ser alhora viva i esborrada.
   */
  @Transaction
  open suspend fun insertLive(entry: EntryEntity): Long {
    clearTombstone(entry.syncId)
    return insert(entry)
  }

  @Transaction
  open suspend fun deleteWithTombstone(
    entry: EntryEntity,
    now: Long,
  ) {
    tombstone(entry.id, now)
    delete(entry)
  }

  @Query("SELECT * FROM entry WHERE id = :id")
  abstract suspend fun get(id: Long): EntryEntity?

  // Només per al motor de sincronització: res d'això segella la fila

  @Query("SELECT * FROM entry WHERE syncId = :syncId")
  abstract suspend fun getBySyncId(syncId: String): EntryEntity?

  @Query("SELECT * FROM entry WHERE dirty = 1")
  abstract suspend fun dirtyRows(): List<EntryEntity>

  @Query("SELECT COUNT(*) FROM entry WHERE dirty = 1")
  abstract fun observeDirtyCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM entry")
  abstract suspend fun count(): Int

  /** Només si la fila continua tal com es va pujar: una edició feta mentrestant es queda pendent. */
  @Query("UPDATE entry SET dirty = 0 WHERE syncId = :syncId AND modifiedAt = :modifiedAt")
  abstract suspend fun markClean(
    syncId: String,
    modifiedAt: Long,
  ): Int

  /** Només per al motor: esborra una entrada que un altre dispositiu ha esborrat, **sense** tombstone. */
  @Query("DELETE FROM entry WHERE syncId = :syncId")
  abstract suspend fun deleteFromRemote(syncId: String)

  /** Només per al motor: desa una entrada baixada, per `syncId`, **sense segellar**. Conserva l'id local. */
  @Transaction
  open suspend fun applyRemote(entry: EntryEntity) {
    val existing = getBySyncId(entry.syncId)
    if (existing == null) insert(entry.copy(id = 0)) else update(entry.copy(id = existing.id))
  }

  /** Grups de canal de les entrades de la persona (derivats, no desats). */
  @Query("SELECT DISTINCT channelType FROM entry WHERE personId = :personId")
  abstract fun observeChannelTypes(personId: Long): Flow<List<ChannelType>>

  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) SELECT syncId, :now, 1 FROM entry " +
      "WHERE personId = :personId AND channelType = :channelType AND occurredAt >= :from AND occurredAt < :to",
  )
  protected abstract suspend fun tombstoneInRange(
    personId: Long,
    channelType: ChannelType,
    from: Long,
    to: Long,
    now: Long,
  )

  /** Mai sol: [deleteInRangeWithTombstones]. */
  @Query(
    "DELETE FROM entry WHERE personId = :personId AND channelType = :channelType AND occurredAt >= :from AND occurredAt < :to",
  )
  protected abstract suspend fun deleteInRange(
    personId: Long,
    channelType: ChannelType,
    from: Long,
    to: Long,
  ): Int

  @Transaction
  open suspend fun deleteInRangeWithTombstones(
    personId: Long,
    channelType: ChannelType,
    from: Long,
    to: Long,
    now: Long,
  ): Int {
    tombstoneInRange(personId, channelType, from, to, now)
    return deleteInRange(personId, channelType, from, to)
  }

  @Query(
    "SELECT COUNT(*) FROM entry WHERE personId = :personId AND channelType = :channelType AND occurredAt >= :from AND occurredAt < :to",
  )
  abstract suspend fun countInRange(
    personId: Long,
    channelType: ChannelType,
    from: Long,
    to: Long,
  ): Int

  @Query(
    """
    SELECT * FROM entry
    WHERE personId = :personId AND kind IN (:kinds)
    ORDER BY occurredAt DESC, id DESC
    """,
  )
  abstract fun observeTimeline(
    personId: Long,
    kinds: Set<EntryKind>,
  ): Flow<List<EntryEntity>>

  @Query("SELECT kind AS kind, COUNT(*) AS count FROM entry WHERE personId = :personId GROUP BY kind")
  abstract fun observeCounts(personId: Long): Flow<List<KindCount>>
}

@Dao
abstract class TaskDao {
  @Insert
  abstract suspend fun insert(task: TaskEntity): Long

  @Update
  abstract suspend fun update(task: TaskEntity)

  /** Mai sol: [deleteWithTombstone] és l'únic esborrat d'una tasca. */
  @Delete
  protected abstract suspend fun delete(task: TaskEntity)

  /** El `syncId` surt de la fila desada, no de l'entitat que arriba. */
  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) SELECT syncId, :now, 1 FROM task WHERE id = :id",
  )
  protected abstract suspend fun tombstone(
    id: Long,
    now: Long,
  )

  @Transaction
  open suspend fun deleteWithTombstone(
    task: TaskEntity,
    now: Long,
  ) {
    tombstone(task.id, now)
    delete(task)
  }

  @Query("SELECT * FROM task WHERE id = :id")
  abstract suspend fun get(id: Long): TaskEntity?

  // Només per al motor de sincronització: res d'això segella la fila

  @Query("SELECT * FROM task WHERE syncId = :syncId")
  abstract suspend fun getBySyncId(syncId: String): TaskEntity?

  @Query("SELECT * FROM task WHERE dirty = 1")
  abstract suspend fun dirtyRows(): List<TaskEntity>

  @Query("SELECT COUNT(*) FROM task WHERE dirty = 1")
  abstract fun observeDirtyCount(): Flow<Int>

  /** Només si la fila continua tal com es va pujar: una edició feta mentrestant es queda pendent. */
  @Query("UPDATE task SET dirty = 0 WHERE syncId = :syncId AND modifiedAt = :modifiedAt")
  abstract suspend fun markClean(
    syncId: String,
    modifiedAt: Long,
  ): Int

  /** Només per al motor: esborra una tasca que un altre dispositiu ha esborrat, **sense** tombstone. */
  @Query("DELETE FROM task WHERE syncId = :syncId")
  abstract suspend fun deleteFromRemote(syncId: String)

  /** Només per al motor: desa una tasca baixada, per `syncId`, **sense segellar**. Conserva l'id local. */
  @Transaction
  open suspend fun applyRemote(task: TaskEntity) {
    val existing = getBySyncId(task.syncId)
    if (existing == null) insert(task.copy(id = 0)) else update(task.copy(id = existing.id))
  }

  /** Pendents (9d): primer les que tenen data, de la més antiga a la més nova, i després les que no en tenen. */
  @Query(
    """
    SELECT * FROM task
    WHERE personId = :personId AND doneAt IS NULL
    ORDER BY dueDate IS NULL, dueDate, createdAt, id
    """,
  )
  abstract fun observeOpen(personId: Long): Flow<List<TaskEntity>>

  /** Fetes (9f), la més recent primer. */
  @Query("SELECT * FROM task WHERE personId = :personId AND doneAt IS NOT NULL ORDER BY doneAt DESC, id DESC")
  abstract fun observeDone(personId: Long): Flow<List<TaskEntity>>

  /** `doneAt = null` la torna a pendents («Desfés» o tocar el check d'una feta). */
  @Query("UPDATE task SET doneAt = :doneAt, modifiedAt = :now, dirty = 1 WHERE id = :id")
  abstract suspend fun setDoneAt(
    id: Long,
    doneAt: Long?,
    now: Long,
  )
}

@Dao
abstract class CustomChannelDao {
  @Insert
  abstract suspend fun insert(channel: CustomChannelEntity): Long

  @Update
  abstract suspend fun update(channel: CustomChannelEntity)

  /** Mai sol: [deleteWithTombstone] és l'únic esborrat d'un canal. */
  @Delete
  protected abstract suspend fun delete(channel: CustomChannelEntity)

  /** El `syncId` surt de la fila desada, no de l'entitat que arriba. */
  @Query(
    "INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty) " +
      "SELECT syncId, :now, 1 FROM custom_channel WHERE id = :id",
  )
  protected abstract suspend fun tombstone(
    id: Long,
    now: Long,
  )

  @Query("UPDATE entry SET modifiedAt = :now, dirty = 1 WHERE customChannelId = :channelId")
  protected abstract suspend fun stampEntriesOf(
    channelId: Long,
    now: Long,
  )

  /**
   * Les entrades que el feien servir es queden al seu grup (`ON DELETE SET NULL`). Com que perden el canal, queden
   * pendents de pujar: si no, l'altre dispositiu les continuaria veient al canal esborrat.
   */
  @Transaction
  open suspend fun deleteWithTombstone(
    channel: CustomChannelEntity,
    now: Long,
  ) {
    stampEntriesOf(channel.id, now)
    tombstone(channel.id, now)
    delete(channel)
  }

  @Query("SELECT * FROM custom_channel WHERE id = :id")
  abstract suspend fun get(id: Long): CustomChannelEntity?

  // Només per al motor de sincronització: res d'això segella la fila

  @Query("SELECT * FROM custom_channel WHERE syncId = :syncId")
  abstract suspend fun getBySyncId(syncId: String): CustomChannelEntity?

  @Query("SELECT * FROM custom_channel")
  abstract suspend fun all(): List<CustomChannelEntity>

  @Query("SELECT * FROM custom_channel WHERE dirty = 1")
  abstract suspend fun dirtyRows(): List<CustomChannelEntity>

  @Query("SELECT COUNT(*) FROM custom_channel WHERE dirty = 1")
  abstract fun observeDirtyCount(): Flow<Int>

  /** El mateix índex únic que vigila `custom_channel`: un altre canal amb el mateix grup i nom exactes. */
  @Query("SELECT * FROM custom_channel WHERE channelType = :channelType AND name = :name AND syncId != :syncId")
  abstract suspend fun findClash(
    channelType: ChannelType,
    name: String,
    syncId: String,
  ): CustomChannelEntity?

  /** Només si la fila continua tal com es va pujar: una edició feta mentrestant es queda pendent. */
  @Query("UPDATE custom_channel SET dirty = 0 WHERE syncId = :syncId AND modifiedAt = :modifiedAt")
  abstract suspend fun markClean(
    syncId: String,
    modifiedAt: Long,
  ): Int

  /** Només per al motor: esborra un canal que un altre dispositiu ha esborrat, **sense** tombstone. */
  @Query("DELETE FROM custom_channel WHERE syncId = :syncId")
  abstract suspend fun deleteFromRemote(syncId: String)

  /**
   * Només per al motor: desa un canal baixat, per `syncId`, **sense segellar**. Conserva l'id local. No comprova
   * l'índex únic: el motor ha de mirar [findClash] abans.
   */
  @Transaction
  open suspend fun applyRemote(channel: CustomChannelEntity) {
    val existing = getBySyncId(channel.syncId)
    if (existing == null) insert(channel.copy(id = 0)) else update(channel.copy(id = existing.id))
  }

  @Query("UPDATE custom_channel SET syncId = :newSyncId, dirty = 0 WHERE id = :id")
  protected abstract suspend fun rename(
    id: Long,
    newSyncId: String,
  )

  /**
   * Només per al motor: un canal propi repetit en dos dispositius. El local passa a ser el `syncId` del remot (el més
   * petit guanya) conservant l'id, perquè les entrades no s'hagin de moure. L'antic `syncId` deixa un tombstone, per si
   * ja s'havia pujat, i les entrades que hi apuntaven queden pendents, perquè ara apunten a un altre `syncId`.
   */
  @Transaction
  open suspend fun adoptSyncId(
    id: Long,
    newSyncId: String,
    now: Long,
  ) {
    stampEntriesOf(id, now)
    tombstone(id, now)
    rename(id, newSyncId)
  }

  @Query(
    """
    SELECT c.*, (SELECT COUNT(*) FROM entry WHERE customChannelId = c.id) AS entryCount
    FROM custom_channel c
    ORDER BY c.name COLLATE NOCASE
    """,
  )
  abstract fun observeAll(): Flow<List<CustomChannelWithCount>>

  @Query(
    """
    SELECT COUNT(*) FROM custom_channel
    WHERE channelType = :channelType AND id != :excludeId AND name = :name COLLATE NOCASE
    """,
  )
  abstract suspend fun countNameConflicts(
    channelType: ChannelType,
    name: String,
    excludeId: Long,
  ): Int

  /** Les entrades canvien de grup, i per tant queden pendents de pujar. */
  @Query(
    "UPDATE entry SET channelType = :channelType, modifiedAt = :now, dirty = 1 WHERE customChannelId = :channelId",
  )
  protected abstract suspend fun moveEntries(
    channelId: Long,
    channelType: ChannelType,
    now: Long,
  )

  /** Canvia el canal de grup i hi mou les seves entrades, perquè el filtre per grups quadri. */
  @Transaction
  open suspend fun changeGroup(
    channel: CustomChannelEntity,
    channelType: ChannelType,
    now: Long,
  ) {
    update(channel.copy(channelType = channelType))
    moveEntries(channel.id, channelType, now)
  }
}

@Dao
interface PinnedFactDao {
  /** Els fets de la persona, per pujar-los dins del seu registre. */
  @Query("SELECT * FROM pinned_fact WHERE personId = :personId ORDER BY position")
  suspend fun getForPerson(personId: Long): List<PinnedFactEntity>

  @Query("SELECT * FROM pinned_fact WHERE personId = :personId ORDER BY position")
  fun observeForPerson(personId: Long): Flow<List<PinnedFactEntity>>
}

/** Els tombstones, només per al motor de sincronització; els esborrats de l'app els escriuen els DAOs de cada taula. */
@Dao
interface SyncTombstoneDao {
  @Query("SELECT * FROM sync_tombstone WHERE dirty = 1")
  suspend fun dirty(): List<SyncTombstoneEntity>

  @Query("SELECT * FROM sync_tombstone WHERE syncId = :syncId")
  suspend fun get(syncId: String): SyncTombstoneEntity?

  @Query("SELECT COUNT(*) FROM sync_tombstone WHERE dirty = 1")
  fun observeDirtyCount(): Flow<Int>

  /** Només si és el que s'ha pujat: un esborrat nou del mateix `syncId` es queda pendent. */
  @Query("DELETE FROM sync_tombstone WHERE syncId = :syncId AND deletedAt = :deletedAt")
  suspend fun deleteUploaded(
    syncId: String,
    deletedAt: Long,
  )

  /** Una fila que torna a existir no pot ser alhora esborrada. */
  @Query("DELETE FROM sync_tombstone WHERE syncId = :syncId")
  suspend fun clear(syncId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun put(tombstone: SyncTombstoneEntity)
}

package com.alorma.contactnotes.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
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

  @Delete
  abstract suspend fun delete(person: PersonEntity)

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
      e.customChannelId AS last_customChannelId, e.isPending AS last_isPending, e.doneAt AS last_doneAt,
      (SELECT COUNT(*) FROM entry WHERE personId = p.id AND isPending = 1 AND doneAt IS NULL) AS openFollowUps
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
interface EntryDao {
  @Insert
  suspend fun insert(entry: EntryEntity): Long

  @Update
  suspend fun update(entry: EntryEntity)

  @Delete
  suspend fun delete(entry: EntryEntity)

  @Query("SELECT * FROM entry WHERE id = :id")
  suspend fun get(id: Long): EntryEntity?

  /** Grups de canal de les entrades de la persona (derivats, no desats). */
  @Query("SELECT DISTINCT channelType FROM entry WHERE personId = :personId")
  fun observeChannelTypes(personId: Long): Flow<List<ChannelType>>

  @Query(
    "DELETE FROM entry WHERE personId = :personId AND channelType = :channelType AND occurredAt >= :from AND occurredAt < :to",
  )
  suspend fun deleteInRange(
    personId: Long,
    channelType: ChannelType,
    from: Long,
    to: Long,
  ): Int

  @Query(
    "SELECT COUNT(*) FROM entry WHERE personId = :personId AND channelType = :channelType AND occurredAt >= :from AND occurredAt < :to",
  )
  suspend fun countInRange(
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
  fun observeTimeline(
    personId: Long,
    kinds: Set<EntryKind>,
  ): Flow<List<EntryEntity>>

  @Query("SELECT kind AS kind, COUNT(*) AS count FROM entry WHERE personId = :personId GROUP BY kind")
  fun observeCounts(personId: Long): Flow<List<KindCount>>

  /** Pendents sense fer de la persona, del més recent al més antic (secció «Pendents» del detall). */
  @Query(
    """
    SELECT * FROM entry
    WHERE personId = :personId AND isPending = 1 AND doneAt IS NULL
    ORDER BY occurredAt DESC, id DESC
    """,
  )
  fun observeOpenFollowUps(personId: Long): Flow<List<EntryEntity>>

  /** `doneAt = null` desfà el «fet»; l'entrada continua pendent. */
  @Query("UPDATE entry SET doneAt = :doneAt WHERE id = :id AND isPending = 1")
  suspend fun setDoneAt(
    id: Long,
    doneAt: Long?,
  )
}

@Dao
abstract class CustomChannelDao {
  @Insert
  abstract suspend fun insert(channel: CustomChannelEntity): Long

  @Update
  abstract suspend fun update(channel: CustomChannelEntity)

  /** Les entrades que el feien servir es queden al seu grup (`ON DELETE SET NULL`). */
  @Delete
  abstract suspend fun delete(channel: CustomChannelEntity)

  @Query("SELECT * FROM custom_channel WHERE id = :id")
  abstract suspend fun get(id: Long): CustomChannelEntity?

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

  @Query("UPDATE entry SET channelType = :channelType WHERE customChannelId = :channelId")
  protected abstract suspend fun moveEntries(
    channelId: Long,
    channelType: ChannelType,
  )

  /** Canvia el canal de grup i hi mou les seves entrades, perquè el filtre per grups quadri. */
  @Transaction
  open suspend fun changeGroup(
    channel: CustomChannelEntity,
    channelType: ChannelType,
  ) {
    update(channel.copy(channelType = channelType))
    moveEntries(channel.id, channelType)
  }
}

@Dao
interface PinnedFactDao {
  @Query("SELECT * FROM pinned_fact WHERE personId = :personId ORDER BY position")
  fun observeForPerson(personId: Long): Flow<List<PinnedFactEntity>>
}

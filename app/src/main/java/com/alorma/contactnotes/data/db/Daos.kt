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

  /** Persones amb un contacte vinculat, també protegides i arxivades (`docs/contacts`). */
  @Query("SELECT * FROM person WHERE contactId IS NOT NULL AND contactLookupKey IS NOT NULL ORDER BY id")
  abstract fun observeLinked(): Flow<List<PersonEntity>>

  /**
   * Desa l'id, el lookup key i el nom nous del contacte, però només si la persona encara hi és vinculada amb
   * [oldLookupKey]: si entremig l'han desvinculada o vinculada a un altre, no es toca.
   */
  @Query(
    """
    UPDATE person SET contactId = :contactId, contactLookupKey = :lookupKey, contactName = :name
    WHERE id = :personId AND contactLookupKey = :oldLookupKey
    """,
  )
  abstract suspend fun refreshContact(
    personId: Long,
    oldLookupKey: String,
    contactId: Long,
    lookupKey: String,
    name: String,
  )

  @Transaction
  @Query(
    """
    SELECT p.*,
      e.id AS last_id, e.personId AS last_personId, e.kind AS last_kind,
      e.channelType AS last_channelType, e.text AS last_text, e.occurredAt AS last_occurredAt
    FROM person p
    LEFT JOIN entry e ON e.id = (
      SELECT id FROM entry WHERE personId = p.id ORDER BY occurredAt DESC, id DESC LIMIT 1
    )
    WHERE p.archived = :archived
    ORDER BY COALESCE(e.occurredAt, p.createdAt) DESC
    """,
  )
  abstract fun observeList(archived: Boolean): Flow<List<PersonListItem>>

  /** Persones amb vincle a Contactes, també les protegides i arxivades: només es compta, no es llegeix cap dada. */
  @Query("SELECT COUNT(*) FROM person WHERE contactLookupKey IS NOT NULL")
  abstract fun observeLinkedCount(): Flow<Int>

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

  /**
   * Les últimes [limit] entrades de cada persona **no arxivada i no protegida**, per al rellotge.
   * El filtre de protegides és aquí i també al constructor de l'instantani: una protegida no ha de sortir mai.
   */
  @Query(
    """
    SELECT id, personId, kind, channelType, text, occurredAt FROM (
      SELECT e.*, ROW_NUMBER() OVER (PARTITION BY e.personId ORDER BY e.occurredAt DESC, e.id DESC) AS position
      FROM entry e
      JOIN person p ON p.id = e.personId
      WHERE p.archived = 0 AND p.isProtected = 0
    )
    WHERE position <= :limit
    ORDER BY personId, occurredAt DESC, id DESC
    """,
  )
  fun observeRecentForWear(limit: Int): Flow<List<EntryEntity>>

  /** Entrades de la persona estrictament anteriors al cursor ([beforeOccurredAt], [beforeId]), de la més nova a la més antiga. */
  @Query(
    """
    SELECT * FROM entry
    WHERE personId = :personId AND (occurredAt < :beforeOccurredAt OR (occurredAt = :beforeOccurredAt AND id < :beforeId))
    ORDER BY occurredAt DESC, id DESC
    LIMIT :limit
    """,
  )
  suspend fun getPageBefore(
    personId: Long,
    beforeOccurredAt: Long,
    beforeId: Long,
    limit: Int,
  ): List<EntryEntity>

  @Query("SELECT kind AS kind, COUNT(*) AS count FROM entry WHERE personId = :personId GROUP BY kind")
  fun observeCounts(personId: Long): Flow<List<KindCount>>
}

@Dao
interface PinnedFactDao {
  @Query("SELECT * FROM pinned_fact WHERE personId = :personId ORDER BY position")
  fun observeForPerson(personId: Long): Flow<List<PinnedFactEntity>>
}

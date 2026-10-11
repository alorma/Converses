package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryDao
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.KindCount
import com.alorma.contactnotes.data.db.PersonDao
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.data.db.PinnedFactDao
import com.alorma.contactnotes.data.db.PinnedFactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ConversesRepository(
  private val personDao: PersonDao,
  private val entryDao: EntryDao,
  private val pinnedFactDao: PinnedFactDao,
) {
  /** Les persones protegides no exposen última entrada ni canals. */
  fun observePeople(archived: Boolean = false): Flow<List<PersonListItem>> =
    personDao.observeList(archived).map { items ->
      items.map { item ->
        if (item.person.isProtected) item.copy(lastEntry = null, channels = emptyList()) else item
      }
    }

  fun observePerson(id: Long): Flow<PersonEntity?> = personDao.observe(id)

  /** Grups de canal usats a les entrades de la persona, sempre en l'ordre fix de [ChannelType]. */
  fun observeChannels(personId: Long): Flow<List<ChannelType>> =
    entryDao.observeChannelTypes(personId).map { it.distinct().sorted() }

  fun observePinnedFacts(personId: Long): Flow<List<PinnedFactEntity>> = pinnedFactDao.observeForPerson(personId)

  fun observeTimeline(
    personId: Long,
    kinds: Set<EntryKind> = EntryKind.entries.toSet(),
  ): Flow<List<EntryEntity>> = entryDao.observeTimeline(personId, kinds)

  fun observeCounts(personId: Long): Flow<List<KindCount>> = entryDao.observeCounts(personId)

  suspend fun savePerson(
    person: PersonEntity,
    facts: List<PinnedFactEntity>,
  ): Long = personDao.save(person, facts)

  suspend fun deletePerson(person: PersonEntity) = personDao.delete(person)

  suspend fun isAliasTaken(
    alias: String,
    excludePersonId: Long = 0,
  ): Boolean = personDao.countAliasConflicts(alias.trim(), excludePersonId) > 0

  suspend fun addEntry(entry: EntryEntity): Long = entryDao.insert(entry)

  suspend fun updateEntry(entry: EntryEntity) = entryDao.update(entry)

  suspend fun deleteEntry(entry: EntryEntity) = entryDao.delete(entry)

  suspend fun moveEntry(
    entry: EntryEntity,
    toPersonId: Long,
  ) = entryDao.update(entry.copy(personId = toPersonId))

  suspend fun setArchived(
    person: PersonEntity,
    archived: Boolean,
  ) = personDao.update(person.copy(archived = archived, updatedAt = System.currentTimeMillis()))

  suspend fun setProtected(
    person: PersonEntity,
    isProtected: Boolean,
  ) = personDao.update(person.copy(isProtected = isProtected, updatedAt = System.currentTimeMillis()))

  suspend fun countEntriesInRange(
    personId: Long,
    channel: ChannelType,
    from: Long,
    to: Long,
  ): Int = entryDao.countInRange(personId, channel, from, to)

  suspend fun deleteEntriesInRange(
    personId: Long,
    channel: ChannelType,
    from: Long,
    to: Long,
  ): Int = entryDao.deleteInRange(personId, channel, from, to)

  suspend fun getEntries(personId: Long): List<EntryEntity> =
    entryDao.observeTimeline(personId, EntryKind.entries.toSet()).first()
}

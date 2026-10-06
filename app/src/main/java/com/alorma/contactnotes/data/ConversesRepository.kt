package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.ChannelDao
import com.alorma.contactnotes.data.db.ChannelEntity
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

class ConversesRepository(
  private val personDao: PersonDao,
  private val channelDao: ChannelDao,
  private val entryDao: EntryDao,
  private val pinnedFactDao: PinnedFactDao,
) {
  fun observePeople(): Flow<List<PersonListItem>> = personDao.observeList()

  fun observePerson(id: Long): Flow<PersonEntity?> = personDao.observe(id)

  fun observeChannels(personId: Long): Flow<List<ChannelEntity>> = channelDao.observeForPerson(personId)

  fun observePinnedFacts(personId: Long): Flow<List<PinnedFactEntity>> =
    pinnedFactDao.observeForPerson(personId)

  fun observeTimeline(personId: Long, kinds: Set<EntryKind> = EntryKind.entries.toSet()): Flow<List<EntryEntity>> =
    entryDao.observeTimeline(personId, kinds)

  fun observeCounts(personId: Long): Flow<List<KindCount>> = entryDao.observeCounts(personId)

  suspend fun savePerson(
    person: PersonEntity,
    channels: List<ChannelEntity>,
    facts: List<PinnedFactEntity>,
  ): Long = personDao.save(person, channels, facts)

  suspend fun deletePerson(person: PersonEntity) = personDao.delete(person)

  suspend fun isAliasTaken(alias: String, excludePersonId: Long = 0): Boolean =
    personDao.countAliasConflicts(alias.trim(), excludePersonId) > 0

  suspend fun addEntry(entry: EntryEntity): Long = entryDao.insert(entry)

  suspend fun updateEntry(entry: EntryEntity) = entryDao.update(entry)

  suspend fun deleteEntry(entry: EntryEntity) = entryDao.delete(entry)
}

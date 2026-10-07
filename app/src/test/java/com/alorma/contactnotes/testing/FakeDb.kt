package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ChannelDao
import com.alorma.contactnotes.data.db.ChannelEntity
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
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Base de dades en memòria que fan servir els DAOs falsos; evita Room i Robolectric als tests de ViewModel. */
class FakeDb {
  val people = MutableStateFlow<List<PersonEntity>>(emptyList())
  val channels = MutableStateFlow<List<ChannelEntity>>(emptyList())
  val entries = MutableStateFlow<List<EntryEntity>>(emptyList())
  val facts = MutableStateFlow<List<PinnedFactEntity>>(emptyList())
  private var next = 1L

  fun nextId(): Long = next++

  val repository = ConversesRepository(FakePersonDao(this), FakeChannelDao(this), FakeEntryDao(this), FakePinnedFactDao(this))

  fun addPerson(
    name: String,
    alias: String? = null,
    isProtected: Boolean = false,
    archived: Boolean = false,
    createdAt: Long = 1_000,
  ): PersonEntity {
    val person = PersonEntity(
      id = nextId(),
      name = name,
      alias = alias,
      avatarShape = AvatarShape.Cookie9Sided,
      avatarColor = AvatarColor.Lavender,
      isProtected = isProtected,
      archived = archived,
      createdAt = createdAt,
      updatedAt = createdAt,
    )
    people.update { it + person }
    return person
  }

  fun addChannel(personId: Long, type: ChannelType): ChannelEntity {
    val channel = ChannelEntity(id = nextId(), personId = personId, type = type)
    channels.update { it + channel }
    return channel
  }

  fun addEntry(
    personId: Long,
    kind: EntryKind = EntryKind.SAID,
    channel: ChannelType = ChannelType.WHATSAPP,
    text: String = "text",
    occurredAt: Long = 10_000,
  ): EntryEntity {
    val entry = EntryEntity(id = nextId(), personId = personId, kind = kind, channelType = channel, text = text, occurredAt = occurredAt)
    entries.update { it + entry }
    return entry
  }

  fun person(id: Long): PersonEntity? = people.value.firstOrNull { it.id == id }
}

class FakePersonDao(private val db: FakeDb) : PersonDao() {
  override suspend fun insert(person: PersonEntity): Long {
    val id = if (person.id == 0L) db.nextId() else person.id
    db.people.update { it + person.copy(id = id) }
    return id
  }

  override suspend fun update(person: PersonEntity) {
    db.people.update { list -> list.map { if (it.id == person.id) person else it } }
  }

  override suspend fun delete(person: PersonEntity) {
    db.people.update { list -> list.filterNot { it.id == person.id } }
    db.channels.update { list -> list.filterNot { it.personId == person.id } }
    db.entries.update { list -> list.filterNot { it.personId == person.id } }
    db.facts.update { list -> list.filterNot { it.personId == person.id } }
  }

  override suspend fun insertChannels(channels: List<ChannelEntity>) {
    db.channels.update { list -> list + channels.map { it.copy(id = db.nextId()) } }
  }

  override suspend fun deleteChannels(personId: Long) {
    db.channels.update { list -> list.filterNot { it.personId == personId } }
  }

  override suspend fun insertFacts(facts: List<PinnedFactEntity>) {
    db.facts.update { list -> list + facts.map { it.copy(id = db.nextId()) } }
  }

  override suspend fun deleteFacts(personId: Long) {
    db.facts.update { list -> list.filterNot { it.personId == personId } }
  }

  override fun observe(id: Long): Flow<PersonEntity?> = db.people.map { list -> list.firstOrNull { it.id == id } }

  override suspend fun get(id: Long): PersonEntity? = db.person(id)

  override fun observeList(archived: Boolean): Flow<List<PersonListItem>> =
    combine(db.people, db.entries, db.channels) { people, entries, channels ->
      people.filter { it.archived == archived }.map { person ->
        val last = entries.filter { it.personId == person.id }.maxWithOrNull(compareBy({ it.occurredAt }, { it.id }))
        PersonListItem(person, last, channels.filter { it.personId == person.id })
      }.sortedByDescending { it.lastEntry?.occurredAt ?: it.person.createdAt }
    }

  override suspend fun countAliasConflicts(alias: String, excludeId: Long): Int =
    db.people.value.count { !it.archived && it.id != excludeId && it.alias.equals(alias, ignoreCase = true) }
}

class FakeChannelDao(private val db: FakeDb) : ChannelDao {
  override fun observeForPerson(personId: Long): Flow<List<ChannelEntity>> =
    db.channels.map { list -> list.filter { it.personId == personId }.sortedBy { it.id } }
}

class FakeEntryDao(private val db: FakeDb) : EntryDao {
  override suspend fun insert(entry: EntryEntity): Long {
    val id = if (entry.id == 0L) db.nextId() else entry.id
    db.entries.update { it + entry.copy(id = id) }
    return id
  }

  override suspend fun update(entry: EntryEntity) {
    db.entries.update { list -> list.map { if (it.id == entry.id) entry else it } }
  }

  override suspend fun delete(entry: EntryEntity) {
    db.entries.update { list -> list.filterNot { it.id == entry.id } }
  }

  override suspend fun get(id: Long): EntryEntity? = db.entries.value.firstOrNull { it.id == id }

  private fun inRange(e: EntryEntity, personId: Long, channelType: ChannelType, from: Long, to: Long) =
    e.personId == personId && e.channelType == channelType && e.occurredAt >= from && e.occurredAt < to

  override suspend fun deleteInRange(personId: Long, channelType: ChannelType, from: Long, to: Long): Int {
    val doomed = db.entries.value.filter { inRange(it, personId, channelType, from, to) }
    db.entries.update { list -> list - doomed.toSet() }
    return doomed.size
  }

  override suspend fun countInRange(personId: Long, channelType: ChannelType, from: Long, to: Long): Int =
    db.entries.value.count { inRange(it, personId, channelType, from, to) }

  override fun observeTimeline(personId: Long, kinds: Set<EntryKind>): Flow<List<EntryEntity>> =
    db.entries.map { list ->
      list.filter { it.personId == personId && it.kind in kinds }
        .sortedWith(compareByDescending<EntryEntity> { it.occurredAt }.thenByDescending { it.id })
    }

  override fun observeCounts(personId: Long): Flow<List<KindCount>> =
    db.entries.map { list ->
      list.filter { it.personId == personId }.groupBy { it.kind }.map { (kind, items) -> KindCount(kind, items.size) }
    }
}

class FakePinnedFactDao(private val db: FakeDb) : PinnedFactDao {
  override fun observeForPerson(personId: Long): Flow<List<PinnedFactEntity>> =
    db.facts.map { list -> list.filter { it.personId == personId }.sortedBy { it.position } }
}

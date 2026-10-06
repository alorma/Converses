package com.alorma.contactnotes.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversesDatabaseTest {
  private lateinit var db: ConversesDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, ConversesDatabase::class.java).build()
  }

  @After
  fun tearDown() = db.close()

  private fun person(name: String, alias: String? = null, archived: Boolean = false, createdAt: Long = 0) =
    PersonEntity(
      name = name,
      alias = alias,
      avatarShape = AvatarShape.Flower,
      avatarColor = AvatarColor.Mint,
      archived = archived,
      createdAt = createdAt,
      updatedAt = createdAt,
    )

  private fun entry(personId: Long, at: Long, kind: EntryKind = EntryKind.SAID) =
    EntryEntity(personId = personId, kind = kind, channelType = ChannelType.WHATSAPP, text = "t$at", occurredAt = at)

  @Test
  fun listIsOrderedByLastEntryAndSkipsArchived() = runTest {
    val a = db.personDao().save(person("A", createdAt = 1), emptyList(), emptyList())
    val b = db.personDao().save(person("B", createdAt = 2), emptyList(), emptyList())
    val c = db.personDao().save(person("C", createdAt = 3), emptyList(), emptyList())
    db.personDao().save(person("Arxivada", archived = true, createdAt = 4), emptyList(), emptyList())
    db.entryDao().insert(entry(a, at = 100))
    db.entryDao().insert(entry(a, at = 500))
    db.entryDao().insert(entry(b, at = 300))

    val list = db.personDao().observeList().first()

    assertEquals(listOf(a, b, c), list.map { it.person.id })
    assertEquals(500L, list.first().lastEntry?.occurredAt)
    assertNull(list.last().lastEntry)
  }

  @Test
  fun saveStoresChannelsAndFactsAndReplacesThemOnUpdate() = runTest {
    val id = db.personDao().save(
      person("A"),
      listOf(ChannelEntity(personId = 0, type = ChannelType.DISCORD, handle = "a#1")),
      listOf(PinnedFactEntity(personId = 0, label = "x", value = "1", position = 0)),
    )
    db.personDao().save(
      db.personDao().get(id)!!,
      listOf(ChannelEntity(personId = 0, type = ChannelType.IN_PERSON)),
      emptyList(),
    )

    assertEquals(listOf(ChannelType.IN_PERSON), db.channelDao().observeForPerson(id).first().map { it.type })
    assertTrue(db.pinnedFactDao().observeForPerson(id).first().isEmpty())
  }

  @Test
  fun deletingPersonCascades() = runTest {
    val id = db.personDao().save(
      person("A"),
      listOf(ChannelEntity(personId = 0, type = ChannelType.PHONE, handle = "1")),
      listOf(PinnedFactEntity(personId = 0, label = "x", value = "1", position = 0)),
    )
    db.entryDao().insert(entry(id, at = 1))

    db.personDao().delete(db.personDao().get(id)!!)

    assertTrue(db.entryDao().observeTimeline(id, EntryKind.entries.toSet()).first().isEmpty())
    assertTrue(db.channelDao().observeForPerson(id).first().isEmpty())
    assertTrue(db.pinnedFactDao().observeForPerson(id).first().isEmpty())
  }

  @Test
  fun timelineFiltersByKindAndCountsGroupByKind() = runTest {
    val id = db.personDao().save(person("A"), emptyList(), emptyList())
    db.entryDao().insert(entry(id, 1, EntryKind.SAID))
    db.entryDao().insert(entry(id, 2, EntryKind.HEARD))
    db.entryDao().insert(entry(id, 3, EntryKind.HEARD))

    val heard = db.entryDao().observeTimeline(id, setOf(EntryKind.HEARD)).first()
    val counts = db.entryDao().observeCounts(id).first().associate { it.kind to it.count }

    assertEquals(listOf(3L, 2L), heard.map { it.occurredAt })
    assertEquals(mapOf(EntryKind.SAID to 1, EntryKind.HEARD to 2), counts)
  }

  @Test
  fun aliasUniquenessIgnoresCaseArchivedAndSelf() = runTest {
    val id = db.personDao().save(person("A", alias = "Marta"), emptyList(), emptyList())
    db.personDao().save(person("B", alias = "Arxiu", archived = true), emptyList(), emptyList())

    assertTrue(db.personDao().countAliasConflicts("marta", excludeId = 0) > 0)
    assertEquals(0, db.personDao().countAliasConflicts("marta", excludeId = id))
    assertEquals(0, db.personDao().countAliasConflicts("arxiu", excludeId = 0))
    assertFalse(db.personDao().countAliasConflicts("altre", excludeId = 0) > 0)
  }
}

package com.alorma.contactnotes.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** L'SQL real de la identitat de sincronització sobre Room en memòria (el `FakeDb` no el prova). */
class SyncIdentityDaoTest {
  private lateinit var db: ConversesDatabase
  private lateinit var people: PersonDao
  private lateinit var entries: EntryDao
  private lateinit var tasks: TaskDao
  private lateinit var channels: CustomChannelDao

  @Before
  fun setUp() {
    db =
      Room
        .inMemoryDatabaseBuilder(
          InstrumentationRegistry.getInstrumentation().targetContext,
          ConversesDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
    people = db.personDao()
    entries = db.entryDao()
    tasks = db.taskDao()
    channels = db.customChannelDao()
  }

  @After
  fun tearDown() = db.close()

  private suspend fun person(name: String = "Anna"): Long =
    people.insert(PersonEntity(name = name, createdAt = 0, updatedAt = 0, dirty = false))

  private suspend fun entry(
    personId: Long,
    customChannelId: Long? = null,
    occurredAt: Long = 0,
  ): Long =
    entries.insert(
      EntryEntity(
        personId = personId,
        kind = EntryKind.SAID,
        channelType = ChannelType.CHAT,
        text = "t",
        occurredAt = occurredAt,
        customChannelId = customChannelId,
        dirty = false,
      ),
    )

  private suspend fun task(personId: Long): Long =
    tasks.insert(TaskEntity(personId = personId, text = "t", createdAt = 0, dirty = false))

  private suspend fun channel(name: String = "WhatsApp"): Long =
    channels.insert(CustomChannelEntity(channelType = ChannelType.CHAT, name = name, createdAt = 0, dirty = false))

  @Test
  fun setDoneAtStampsTheTask() =
    runTest {
      val id = task(person())

      tasks.setDoneAt(id, doneAt = 40, now = 50)

      val task = tasks.get(id)!!
      assertEquals(40L, task.doneAt)
      assertEquals(50L, task.modifiedAt)
      assertTrue(task.dirty)
    }

  @Test
  fun changeGroupStampsTheEntriesOfTheChannelOnly() =
    runTest {
      val anna = person()
      val whatsApp = channel()
      val inside = entry(anna, customChannelId = whatsApp)
      val outside = entry(anna)

      channels.changeGroup(channels.get(whatsApp)!!, ChannelType.SOCIAL, now = 50)

      val moved = entries.get(inside)!!
      assertEquals(ChannelType.SOCIAL, moved.channelType)
      assertEquals(50L, moved.modifiedAt)
      assertTrue(moved.dirty)
      val untouched = entries.get(outside)!!
      assertEquals(ChannelType.CHAT, untouched.channelType)
      assertEquals(0L, untouched.modifiedAt)
      assertFalse(untouched.dirty)
    }

  @Test
  fun observeListReadsTheLastEntryWithItsSyncColumns() =
    runTest {
      val anna = person()
      entry(anna, occurredAt = 10)
      val last = entry(anna, occurredAt = 20)

      val item = people.observeList(archived = false).first().single()

      assertEquals(entries.get(last), item.lastEntry)
      assertEquals(people.get(anna)!!.syncId, item.person.syncId)
    }
}

package com.alorma.contactnotes.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** L'SQL real dels canals propis sobre Room en memòria (el `FakeDb` no el prova). */
class CustomChannelDaoTest {
  private lateinit var db: ConversesDatabase
  private lateinit var channels: CustomChannelDao
  private lateinit var entries: EntryDao
  private lateinit var people: PersonDao

  @Before
  fun setUp() {
    db =
      Room
        .inMemoryDatabaseBuilder(
          InstrumentationRegistry.getInstrumentation().targetContext,
          ConversesDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
    channels = db.customChannelDao()
    entries = db.entryDao()
    people = db.personDao()
  }

  @After
  fun tearDown() = db.close()

  private suspend fun person(name: String = "Anna"): Long =
    people.insert(PersonEntity(name = name, createdAt = 0, updatedAt = 0))

  private suspend fun channel(
    name: String,
    type: ChannelType = ChannelType.CHAT,
  ): Long = channels.insert(CustomChannelEntity(channelType = type, name = name, createdAt = 0))

  private suspend fun entry(
    personId: Long,
    channelType: ChannelType = ChannelType.CHAT,
    customChannelId: Long? = null,
    occurredAt: Long = 0,
  ): Long =
    entries.insert(
      EntryEntity(
        personId = personId,
        kind = EntryKind.SAID,
        channelType = channelType,
        text = "t",
        occurredAt = occurredAt,
        customChannelId = customChannelId,
      ),
    )

  @Test
  fun changeGroupMovesOnlyThatChannelsEntries() =
    runTest {
      val p = person()
      val whatsApp = channel("WhatsApp")
      val signal = channel("Signal")
      val moved = entry(p, customChannelId = whatsApp)
      val other = entry(p, customChannelId = signal)
      val plain = entry(p)

      channels.changeGroup(channels.get(whatsApp)!!, ChannelType.SOCIAL, now = 0)

      assertEquals(ChannelType.SOCIAL, channels.get(whatsApp)!!.channelType)
      assertEquals(ChannelType.SOCIAL, entries.get(moved)!!.channelType)
      assertEquals(ChannelType.CHAT, entries.get(other)!!.channelType)
      assertEquals(ChannelType.CHAT, entries.get(plain)!!.channelType)
    }

  @Test
  fun deletingAChannelKeepsItsEntriesInTheirGroup() =
    runTest {
      val p = person()
      val id = channel("WhatsApp")
      val e = entry(p, customChannelId = id)

      channels.deleteWithTombstone(channels.get(id)!!, now = 0)

      val kept = entries.get(e)!!
      assertNull(kept.customChannelId)
      assertEquals(ChannelType.CHAT, kept.channelType)
    }

  @Test
  fun nameConflictsIgnoreCaseWithinTheGroupOnly() =
    runTest {
      val id = channel("WhatsApp")

      assertEquals(1, channels.countNameConflicts(ChannelType.CHAT, "whatsapp", excludeId = 0))
      assertEquals(1, channels.countNameConflicts(ChannelType.CHAT, "WHATSAPP", excludeId = 0))
      assertEquals(0, channels.countNameConflicts(ChannelType.SOCIAL, "whatsapp", excludeId = 0))
      assertEquals(0, channels.countNameConflicts(ChannelType.CHAT, "whatsapp", excludeId = id))
      assertEquals(0, channels.countNameConflicts(ChannelType.CHAT, "Signal", excludeId = 0))
    }

  @Test
  fun observeAllCountsEntriesAndSortsIgnoringCase() =
    runTest {
      val p = person()
      val b = channel("beta")
      channel("Alpha", ChannelType.SOCIAL)
      entry(p, customChannelId = b)
      entry(p, customChannelId = b)

      val all = channels.observeAll().first()

      assertEquals(listOf("Alpha", "beta"), all.map { it.channel.name })
      assertEquals(listOf(0, 2), all.map { it.entryCount })
    }
}

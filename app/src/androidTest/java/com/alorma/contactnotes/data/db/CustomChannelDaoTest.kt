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

/** L'SQL real dels canals propis i dels pendents sobre Room en memòria (el `FakeDb` no el prova). */
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
    isPending: Boolean = false,
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
        isPending = isPending,
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

      channels.changeGroup(channels.get(whatsApp)!!, ChannelType.SOCIAL)

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

      channels.delete(channels.get(id)!!)

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

  @Test
  fun doneOnlyAppliesToPendingEntriesAndCanBeUndone() =
    runTest {
      val p = person()
      val pending = entry(p, isPending = true)
      val normal = entry(p)

      entries.setDoneAt(pending, 10)
      entries.setDoneAt(normal, 10)

      assertEquals(10L, entries.get(pending)!!.doneAt)
      assertNull(entries.get(normal)!!.doneAt)

      entries.setDoneAt(pending, null)
      assertNull(entries.get(pending)!!.doneAt)
    }

  @Test
  fun openFollowUpsSkipDoneAndNonPendingEntries() =
    runTest {
      val p = person()
      val other = person("Bru")
      val older = entry(p, isPending = true, occurredAt = 1)
      val newer = entry(p, isPending = true, occurredAt = 2)
      val done = entry(p, isPending = true, occurredAt = 3)
      entry(p, occurredAt = 4)
      entry(other, isPending = true)
      entries.setDoneAt(done, 5)

      assertEquals(listOf(newer, older), entries.observeOpenFollowUps(p).first().map { it.id })
    }

  @Test
  fun peopleListCountsOpenFollowUps() =
    runTest {
      val p = person()
      val other = person("Bru")
      entry(p, isPending = true)
      val done = entry(p, isPending = true)
      entry(p)
      entries.setDoneAt(done, 5)

      val counts = people.observeList(archived = false).first().associate { it.person.id to it.openFollowUps }

      assertEquals(1, counts[p])
      assertEquals(0, counts[other])
    }
}

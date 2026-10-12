package com.alorma.contactnotes.wear.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class MemoryQueue : PendingNoteQueue {
  val notes = mutableListOf<WearNote>()

  override suspend fun all() = notes.toList()

  override suspend fun add(note: WearNote) {
    if (notes.none { it.id == note.id }) notes += note
  }

  override suspend fun remove(id: String) {
    notes.removeAll { it.id == id }
  }

  override suspend fun retainOnly(personIds: Set<Long>): Int {
    val before = notes.size
    notes.removeAll { it.personId !in personIds }
    return before - notes.size
  }

  override suspend fun clear() {
    notes.clear()
  }
}

private class FakeSender(
  var answer: (WearNote) -> NoteDelivery = { NoteDelivery.Delivered },
) : NoteSender {
  val sent = mutableListOf<String>()

  override suspend fun send(note: WearNote): NoteDelivery {
    sent += note.id
    return answer(note)
  }
}

class NoteOutboxTest {
  @get:Rule
  val folder = TemporaryFolder()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val queue = MemoryQueue()
  private val sender = FakeSender()
  private var scheduled = 0
  private val outbox = NoteOutbox(queue, sender) { scheduled++ }

  @After
  fun tearDown() = scope.cancel()

  private fun note(
    id: String,
    text: String = "text $id",
    personId: Long = 1,
  ) = WearNote(id = id, personId = personId, who = EntryKind.SAID, text = text, timestamp = 1_000)

  @Test
  fun submitStoresTheNoteAndSchedulesTheDelivery() =
    runTest {
      outbox.submit(note("a"))
      assertEquals(listOf("a"), queue.notes.map { it.id })
      assertEquals(1, scheduled)
      assertTrue(outbox.hasPending())
    }

  @Test
  fun deliveredNotesLeaveTheQueueOldestFirst() =
    runTest {
      outbox.submit(note("a"))
      outbox.submit(note("b"))
      assertTrue(outbox.deliverAll())
      assertEquals(listOf("a", "b"), sender.sent)
      assertFalse(outbox.hasPending())
    }

  @Test
  fun withoutPhoneTheNotesStayAndTheRestIsNotTried() =
    runTest {
      sender.answer = { NoteDelivery.Unavailable }
      outbox.submit(note("a"))
      outbox.submit(note("b"))
      assertFalse(outbox.deliverAll())
      assertEquals(listOf("a"), sender.sent)
      assertEquals(listOf("a", "b"), queue.notes.map { it.id })
    }

  @Test
  fun retryAfterThePhoneComesBackDeliversEverything() =
    runTest {
      sender.answer = { NoteDelivery.Unavailable }
      outbox.submit(note("a"))
      assertFalse(outbox.deliverAll())
      sender.answer = { NoteDelivery.Delivered }
      assertTrue(outbox.deliverAll())
      assertFalse(outbox.hasPending())
    }

  @Test
  fun aRejectedNoteIsDroppedAndDoesNotBlockTheOthers() =
    runTest {
      sender.answer = { if (it.id == "a") NoteDelivery.Rejected else NoteDelivery.Delivered }
      outbox.submit(note("a"))
      outbox.submit(note("b"))
      assertTrue(outbox.deliverAll())
      assertEquals(listOf("a", "b"), sender.sent)
      assertFalse(outbox.hasPending())
    }

  @Test
  fun theSameNoteSubmittedTwiceIsQueuedOnce() =
    runTest {
      outbox.submit(note("a"))
      outbox.submit(note("a"))
      assertEquals(1, queue.notes.size)
    }

  @Test
  fun dataStoreQueueKeepsNotesAcrossInstancesAndMultilineText() =
    runTest {
      val file = folder.newFile("queue.preferences_pb")
      val first = DataStorePendingNoteQueue(PreferenceDataStoreFactory.create(scope = scope) { file })
      first.add(note("a", text = "línia 1\nlínia 2"))
      first.add(note("b"))
      first.add(note("a"))
      first.remove("b")
      assertEquals(listOf("a"), first.all().map { it.id })
      assertEquals("línia 1\nlínia 2", first.all().single().text)
    }

  @Test
  fun dataStoreQueueClearRemovesEveryNote() =
    runTest {
      val queue =
        DataStorePendingNoteQueue(
          PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("clear.preferences_pb") },
        )
      queue.add(note("a"))
      queue.add(note("b"))
      queue.clear()
      assertEquals(emptyList<WearNote>(), queue.all())
    }

  @Test
  fun dataStoreQueueRetainOnlyDropsTheNotesOfOtherPeople() =
    runTest {
      val queue =
        DataStorePendingNoteQueue(
          PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("retain.preferences_pb") },
        )
      queue.add(note("a", personId = 1))
      queue.add(note("b", personId = 2))
      queue.add(note("c", personId = 1))

      assertEquals(1, queue.retainOnly(setOf(1)))
      assertEquals(listOf("a", "c"), queue.all().map { it.id })
      assertEquals(0, queue.retainOnly(setOf(1)))
    }

  @Test
  fun aRetriedNoteAfterARestartIsNotDuplicated() =
    runTest {
      outbox.submit(note("a"))
      sender.answer = { NoteDelivery.Unavailable }
      assertFalse(outbox.deliverAll())

      val restarted = NoteOutbox(queue, sender) { scheduled++ }
      restarted.submit(note("a"))
      sender.answer = { NoteDelivery.Delivered }

      assertTrue(restarted.deliverAll())
      assertEquals(emptyList<WearNote>(), queue.notes)
    }

  @Test
  fun aRejectedNoteIsCountedAsDiscarded() =
    runTest {
      val counter =
        DataStoreDiscardedNotes(
          PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("discarded.preferences_pb") },
        )
      val counting = NoteOutbox(queue, sender, counter) { scheduled++ }
      sender.answer = { if (it.id == "a") NoteDelivery.Rejected else NoteDelivery.Delivered }
      counting.submit(note("a"))
      counting.submit(note("b"))

      assertTrue(counting.deliverAll())

      assertEquals(1, counter.count.first())
      counter.clear()
      assertEquals(0, counter.count.first())
    }
}

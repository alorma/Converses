package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearNote
import com.alorma.contactnotes.shared.WearNoteCodec
import com.alorma.contactnotes.shared.WearNoteResult
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeRegistry : NoteIdRegistry {
  val ids = mutableSetOf<String>()

  override suspend fun contains(id: String) = id in ids

  override suspend fun add(id: String) {
    ids += id
  }
}

class WearNoteReceiverTest {
  private val db = FakeDb()
  private val registry = FakeRegistry()
  private val receiver = WearNoteReceiver(db.repository, registry)

  private suspend fun send(
    personId: Long,
    id: String = "n1",
    who: EntryKind = EntryKind.HEARD,
    text: String = "  Hola  ",
    timestamp: Long = 5_000,
  ): WearNoteResult? {
    val payload =
      WearNoteCodec.encodeNote(
        WearNote(id = id, personId = personId, who = who, text = text, timestamp = timestamp),
      )
    return receiver.receive(payload)?.let { WearNoteCodec.decodeResponse(it)!!.result }
  }

  @Test
  fun savesTheNoteAsANewEntry() =
    runTest {
      val person = db.addPerson("Kael")
      assertEquals(WearNoteResult.SAVED, send(person.id))
      val entry = db.repository.getEntries(person.id).single()
      assertEquals("Hola", entry.text)
      assertEquals(EntryKind.HEARD, entry.kind)
      assertEquals(5_000L, entry.occurredAt)
    }

  @Test
  fun usesTheLastChannelWithThePersonOrTheFirstOne() =
    runTest {
      val kael = db.addPerson("Kael")
      send(kael.id, id = "a")
      assertEquals(
        ChannelType.entries.first(),
        db.repository
          .getEntries(kael.id)
          .single()
          .channelType,
      )
      val marta = db.addPerson("Marta")
      db.addEntry(marta.id, channel = ChannelType.SOCIAL, occurredAt = 9_000)
      send(marta.id, id = "b", timestamp = 10_000)
      assertEquals(
        ChannelType.SOCIAL,
        db.repository
          .getEntries(marta.id)
          .first()
          .channelType,
      )
    }

  @Test
  fun aResentNoteIsNotSavedTwice() =
    runTest {
      val person = db.addPerson("Kael")
      assertEquals(WearNoteResult.SAVED, send(person.id))
      assertEquals(WearNoteResult.SAVED, send(person.id))
      assertEquals(1, db.repository.getEntries(person.id).size)
    }

  @Test
  fun protectedArchivedAndUnknownPeopleAreRejected() =
    runTest {
      val secret = db.addPerson("Secret", isProtected = true)
      val old = db.addPerson("Old", archived = true)
      assertEquals(WearNoteResult.REJECTED, send(secret.id, id = "a"))
      assertEquals(WearNoteResult.REJECTED, send(old.id, id = "b"))
      assertEquals(WearNoteResult.REJECTED, send(999, id = "c"))
      assertEquals(0, db.repository.getEntries(secret.id).size + db.repository.getEntries(old.id).size)
      assertEquals(emptySet<String>(), registry.ids)
    }

  @Test
  fun unreadableOrInvalidPayloadGetsNoAnswer() =
    runTest {
      assertNull(receiver.receive("no és json"))
      val person = db.addPerson("Kael")
      assertNull(send(person.id, text = "   "))
    }
}

package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.WEAR_ENTRIES_PAGE_SIZE
import com.alorma.contactnotes.shared.WearEntriesCodec
import com.alorma.contactnotes.shared.WearEntriesRequest
import com.alorma.contactnotes.shared.WearEntriesResponse
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearEntriesResponderTest {
  private val db = FakeDb()
  private val responder = WearEntriesResponder(db.repository)

  private suspend fun ask(
    personId: Long,
    beforeOccurredAt: Long = Long.MAX_VALUE,
    beforeId: Long = Long.MAX_VALUE,
  ): WearEntriesResponse {
    val payload =
      WearEntriesCodec.encodeRequest(
        WearEntriesRequest(personId = personId, beforeOccurredAt = beforeOccurredAt, beforeId = beforeId),
      )
    return WearEntriesCodec.decodeResponse(responder.respond(payload)!!)!!
  }

  @Test
  fun servesTheNewestPageAndSaysThereIsMore() =
    runTest {
      val person = db.addPerson("Kael")
      repeat(25) { db.addEntry(person.id, text = "e$it", occurredAt = 1_000L + it) }
      val page = ask(person.id)
      assertEquals(WEAR_ENTRIES_PAGE_SIZE, page.entries.size)
      assertEquals("e24", page.entries.first().text)
      assertTrue(page.hasMore)
      assertFalse(page.denied)
    }

  @Test
  fun nextPageContinuesFromTheCursorUntilTheEnd() =
    runTest {
      val person = db.addPerson("Kael")
      repeat(25) { db.addEntry(person.id, occurredAt = 1_000L + it) }
      val first = ask(person.id)
      val last = first.entries.last()
      val second = ask(person.id, last.occurredAt, last.id)
      assertEquals(5, second.entries.size)
      assertFalse(second.hasMore)
      assertTrue(second.entries.all { it.occurredAt < last.occurredAt })
    }

  @Test
  fun entriesWithTheSameTimeAreNotSkippedOrRepeated() =
    runTest {
      val person = db.addPerson("Kael")
      val ids = (1..3).map { db.addEntry(person.id, occurredAt = 5_000).id }
      val second = ask(person.id, 5_000, ids[2])
      assertEquals(listOf(ids[1], ids[0]), second.entries.map { it.id })
    }

  @Test
  fun protectedPersonGetsNothing() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      db.addEntry(person.id, text = "confidencial")
      val raw =
        responder.respond(
          WearEntriesCodec.encodeRequest(
            WearEntriesRequest(personId = person.id, beforeOccurredAt = Long.MAX_VALUE, beforeId = Long.MAX_VALUE),
          ),
        )!!
      assertTrue("confidencial" !in raw)
      assertTrue(WearEntriesCodec.decodeResponse(raw)!!.denied)
    }

  @Test
  fun archivedAndUnknownPeopleGetNothing() =
    runTest {
      val archived = db.addPerson("Vella", archived = true)
      db.addEntry(archived.id)
      assertTrue(ask(archived.id).denied)
      assertTrue(ask(999).denied)
    }

  @Test
  fun unreadableRequestIsNotAnswered() =
    runTest {
      assertNull(responder.respond("no és json"))
    }
}

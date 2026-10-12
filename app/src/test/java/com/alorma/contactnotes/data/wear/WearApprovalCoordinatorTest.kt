package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.WEAR_APPROVAL_TTL_MS
import com.alorma.contactnotes.shared.WEAR_ENTRIES_PAGE_SIZE
import com.alorma.contactnotes.shared.WearApprovalAck
import com.alorma.contactnotes.shared.WearApprovalCancel
import com.alorma.contactnotes.shared.WearApprovalCodec
import com.alorma.contactnotes.shared.WearApprovalDecision
import com.alorma.contactnotes.shared.WearApprovalRequest
import com.alorma.contactnotes.shared.WearApprovalResult
import com.alorma.contactnotes.shared.WearApprovalStatus
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearApprovalCoordinatorTest {
  private val db = FakeDb()
  private var now = 1_000_000L
  private val shown = mutableListOf<String>()
  private val cancelled = mutableListOf<String>()
  private val sent = mutableListOf<Pair<String, String>>()
  private val tokens = ApprovalTokens(clock = { now })

  private val notifier =
    object : ApprovalNotifier {
      override fun show(requestId: String) {
        shown += requestId
      }

      override fun cancel(requestId: String) {
        cancelled += requestId
      }
    }

  private val coordinator =
    WearApprovalCoordinator(
      repository = db.repository,
      notifier = notifier,
      sender = { nodeId, payload -> sent += nodeId to payload },
      tokens = tokens,
      clock = { now },
    )

  private suspend fun request(
    personId: Long,
    requestId: String = "req-1",
    node: String = "watch",
  ): WearApprovalAck =
    WearApprovalCodec.decodeAck(
      coordinator.onRequest(
        node,
        WearApprovalCodec.encodeRequest(WearApprovalRequest(requestId = requestId, personId = personId)),
      )!!,
    )!!

  private fun lastResult(): WearApprovalResult = WearApprovalCodec.decodeResult(sent.last().second)!!

  @Test
  fun aProtectedPersonGetsPendingAndTheGenericNotification() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)

      val ack = request(person.id)

      assertEquals(WearApprovalStatus.PENDING, ack.status)
      assertEquals(listOf("req-1"), shown)
      assertTrue(coordinator.isPending("req-1"))
      assertTrue(sent.isEmpty())
    }

  @Test
  fun nothingToApproveForANormalArchivedOrMissingPerson() =
    runTest {
      val normal = db.addPerson("Kael")
      val archived = db.addPerson("Old", isProtected = true, archived = true)

      assertEquals(WearApprovalStatus.REJECTED, request(normal.id, "a").status)
      assertEquals(WearApprovalStatus.REJECTED, request(archived.id, "b").status)
      assertEquals(WearApprovalStatus.REJECTED, request(999, "c").status)
      assertTrue(shown.isEmpty())
    }

  @Test
  fun unreadableRequestsGetNoAnswer() =
    runTest {
      assertNull(coordinator.onRequest("watch", "no és json"))
    }

  @Test
  fun approvalSendsOnlyThatPersonsEntriesAndAToken() =
    runTest {
      val secret = db.addPerson("Secret", isProtected = true)
      val other = db.addPerson("Other", isProtected = true)
      repeat(25) { db.addEntry(secret.id, text = "s$it", occurredAt = 1_000L + it) }
      db.addEntry(other.id, text = "altre", occurredAt = 5_000)
      request(secret.id)

      assertTrue(coordinator.approve("req-1"))

      assertEquals("watch", sent.single().first)
      val result = lastResult()
      assertEquals(WearApprovalDecision.APPROVED, result.decision)
      assertEquals(WEAR_ENTRIES_PAGE_SIZE, result.entries.size)
      assertEquals("s24", result.entries.first().text)
      assertTrue(result.hasMore)
      assertTrue(result.entries.none { it.text == "altre" })
      assertTrue(tokens.isValid(result.noteToken, secret.id))
      assertFalse(tokens.isValid(result.noteToken, other.id))
      assertEquals(listOf("req-1"), cancelled)
    }

  @Test
  fun anApprovalIsSingleUse() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      request(person.id)

      assertTrue(coordinator.approve("req-1"))
      assertFalse(coordinator.approve("req-1"))

      assertEquals(1, sent.size)
      assertEquals(WearApprovalStatus.REJECTED, request(person.id, "req-1").status)
    }

  @Test
  fun anUnknownRequestServesNothing() =
    runTest {
      assertFalse(coordinator.approve("nope"))
      assertTrue(sent.isEmpty())
    }

  @Test
  fun aRequestThatExpiredBeforeApprovalIsNeitherPendingNorServed() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      db.addEntry(person.id, text = "confidencial")
      request(person.id)

      now += WEAR_APPROVAL_TTL_MS + 1

      assertFalse(coordinator.isPending("req-1"))
      assertFalse(coordinator.approve("req-1"))
      assertTrue(sent.isEmpty())
      assertEquals(listOf("req-1"), cancelled)
    }

  @Test
  fun denyTellsTheWatchWithoutContent() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      db.addEntry(person.id, text = "confidencial")
      request(person.id)

      coordinator.deny("req-1")

      val result = lastResult()
      assertEquals(WearApprovalDecision.DENIED, result.decision)
      assertTrue(result.entries.isEmpty())
      assertNull(result.noteToken)
      assertFalse(coordinator.approve("req-1"))
    }

  @Test
  fun theWatchCancellingInvalidatesTheRequestAndRemovesTheNotification() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      request(person.id)

      coordinator.onCancel(WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = "req-1")))

      assertFalse(coordinator.isPending("req-1"))
      assertEquals(listOf("req-1"), cancelled)
      assertFalse(coordinator.approve("req-1"))
      assertTrue(sent.isEmpty())
    }

  @Test
  fun aPersonWhoGetsArchivedBeforeApprovalIsDenied() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      db.addEntry(person.id, text = "confidencial")
      request(person.id)
      db.repository.setArchived(person, true)

      assertFalse(coordinator.approve("req-1"))

      assertEquals(WearApprovalDecision.DENIED, lastResult().decision)
      assertTrue(lastResult().entries.isEmpty())
    }

  @Test
  fun tooManyPendingRequestsAreRejected() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)

      assertEquals(WearApprovalStatus.PENDING, request(person.id, "a").status)
      assertEquals(WearApprovalStatus.PENDING, request(person.id, "b").status)
      assertEquals(WearApprovalStatus.PENDING, request(person.id, "c").status)
      assertEquals(WearApprovalStatus.REJECTED, request(person.id, "d").status)

      assertEquals(listOf("a", "b", "c"), shown)
    }

  @Test
  fun requestsAreRateLimitedPerMinute() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      repeat(5) { i ->
        assertEquals(WearApprovalStatus.PENDING, request(person.id, "r$i").status)
        coordinator.deny("r$i")
      }

      assertEquals(WearApprovalStatus.REJECTED, request(person.id, "r5").status)

      now += 60_001
      assertEquals(WearApprovalStatus.PENDING, request(person.id, "r6").status)
    }

  @Test
  fun aRepeatedRequestIdIsRejectedEvenWhileStillPending() =
    runTest {
      val person = db.addPerson("Secret", isProtected = true)
      request(person.id)

      assertEquals(WearApprovalStatus.REJECTED, request(person.id).status)
      assertEquals(1, shown.size)
    }

  @Test
  fun notificationsNeverCarryTheName() =
    runTest {
      val person = db.addPerson("Nom Secret", isProtected = true)
      request(person.id)

      // L'únic que rep el notificador és l'identificador de la petició
      assertTrue(shown.none { "Secret" in it })
      assertNotNull(shown.singleOrNull())
    }
}

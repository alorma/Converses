package com.alorma.contactnotes.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearApprovalTest {
  private fun entry(
    id: Long,
    text: String = "text $id",
  ) = WearEntry(id, EntryKind.SAID, ChannelType.CHAT, text, occurredAt = id)

  private fun approved(
    entries: List<WearEntry> = listOf(entry(2), entry(1)),
    token: String? = "tok",
    expiresAt: Long? = 5_000,
  ) = WearApprovalResult(
    requestId = "req-1",
    decision = WearApprovalDecision.APPROVED,
    entries = entries,
    hasMore = true,
    noteToken = token,
    noteTokenExpiresAt = expiresAt,
  )

  @Test
  fun requestAndCancelRoundTrip() {
    val request = WearApprovalRequest(requestId = "req-1", personId = 4)
    assertEquals(request, WearApprovalCodec.decodeRequest(WearApprovalCodec.encodeRequest(request)))
    val cancel = WearApprovalCancel(requestId = "req-1")
    assertEquals(cancel, WearApprovalCodec.decodeCancel(WearApprovalCodec.encodeCancel(cancel)))
  }

  @Test
  fun ackRoundTrip() {
    val ack = WearApprovalAck(requestId = "req-1", status = WearApprovalStatus.PENDING)
    assertEquals(ack, WearApprovalCodec.decodeAck(WearApprovalCodec.encodeAck(ack)))
  }

  @Test
  fun blankOrHugeRequestIdIsRejected() {
    assertNull(
      WearApprovalCodec.decodeRequest(
        WearApprovalCodec.encodeRequest(WearApprovalRequest(requestId = " ", personId = 1)),
      ),
    )
    assertNull(
      WearApprovalCodec.decodeRequest(
        WearApprovalCodec.encodeRequest(WearApprovalRequest(requestId = "x".repeat(65), personId = 1)),
      ),
    )
    assertNull(WearApprovalCodec.decodeCancel(WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = ""))))
  }

  @Test
  fun anotherVersionOrGarbageIsRejected() {
    assertNull(WearApprovalCodec.decodeRequest("""{"version":99,"requestId":"a","personId":1}"""))
    assertNull(WearApprovalCodec.decodeRequest("no és json"))
    assertNull(WearApprovalCodec.decodeAck("no és json"))
    assertNull(WearApprovalCodec.decodeResult("no és json"))
  }

  @Test
  fun approvedResultRoundTripKeepsEntriesNewestFirstAndToken() {
    val result = approved()
    assertEquals(result, WearApprovalCodec.decodeResult(WearApprovalCodec.encodeResult(result)))
  }

  @Test
  fun deniedOrExpiredResultCarriesNothing() {
    for (decision in listOf(WearApprovalDecision.DENIED, WearApprovalDecision.EXPIRED)) {
      val dirty = approved().copy(decision = decision)
      val decoded = WearApprovalCodec.decodeResult(WearApprovalCodec.encodeResult(dirty))!!
      assertEquals(decision, decoded.decision)
      assertTrue(decoded.entries.isEmpty())
      assertNull(decoded.noteToken)
      assertNull(decoded.noteTokenExpiresAt)
      assertEquals(false, decoded.hasMore)
    }
  }

  @Test
  fun decodingAlsoSanitizesAResultNotBuiltByThisApp() {
    val raw =
      """{"version":1,"requestId":"r","decision":"DENIED","entries":[{"id":1,"kind":"SAID","channel":"CHAT","text":"secret","occurredAt":1}],"noteToken":"t","noteTokenExpiresAt":9}"""
    val decoded = WearApprovalCodec.decodeResult(raw)!!
    assertTrue(decoded.entries.isEmpty())
    assertNull(decoded.noteToken)
  }

  @Test
  fun approvedEntriesAreCappedToAPageAndTruncated() {
    val many = (1L..30L).map { entry(it, text = "x".repeat(WEAR_MAX_ENTRY_TEXT_LENGTH + 50)) }
    val decoded = WearApprovalCodec.decodeResult(WearApprovalCodec.encodeResult(approved(entries = many)))!!
    assertEquals(WEAR_ENTRIES_PAGE_SIZE, decoded.entries.size)
    assertEquals(30L, decoded.entries.first().id)
    assertTrue(decoded.entries.all { it.text.length <= WEAR_MAX_ENTRY_TEXT_LENGTH })
  }

  @Test
  fun tokenAndExpiryGoTogether() {
    val noExpiry = WearApprovalCodec.decodeResult(WearApprovalCodec.encodeResult(approved(expiresAt = null)))!!
    assertNull(noExpiry.noteToken)
    val noToken = WearApprovalCodec.decodeResult(WearApprovalCodec.encodeResult(approved(token = null)))!!
    assertNull(noToken.noteTokenExpiresAt)
  }
}

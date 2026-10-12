package com.alorma.contactnotes.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearEntriesPageTest {
  private fun entry(
    id: Long,
    at: Long = id,
    text: String = "text $id",
  ) = WearEntry(id, EntryKind.SAID, ChannelType.CHAT, text, at)

  @Test
  fun requestRoundTrip() {
    val request = WearEntriesRequest(personId = 3, beforeOccurredAt = 50, beforeId = 9)
    assertEquals(request, WearEntriesCodec.decodeRequest(WearEntriesCodec.encodeRequest(request)))
  }

  @Test
  fun requestLimitIsCappedToThePageSize() {
    val raw =
      WearEntriesCodec.encodeRequest(
        WearEntriesRequest(personId = 1, beforeOccurredAt = 1, beforeId = 1, limit = 5000),
      )
    assertEquals(WEAR_ENTRIES_PAGE_SIZE, WearEntriesCodec.decodeRequest(raw)!!.limit)
  }

  @Test
  fun requestWithAnotherVersionOrGarbageIsRejected() {
    assertNull(WearEntriesCodec.decodeRequest("""{"version":99,"personId":1,"beforeOccurredAt":1,"beforeId":1}"""))
    assertNull(WearEntriesCodec.decodeRequest("no és json"))
  }

  @Test
  fun responseRoundTripKeepsEntriesNewestFirst() {
    val response = WearEntriesResponse(entries = listOf(entry(3), entry(2), entry(1)), hasMore = true)
    assertEquals(response, WearEntriesCodec.decodeResponse(WearEntriesCodec.encodeResponse(response)))
  }

  @Test
  fun responseIsCappedToOnePageAndTruncated() {
    val many = (1L..50L).map { entry(it, text = "x".repeat(1_000)) }
    val decoded =
      WearEntriesCodec.decodeResponse(
        WearEntriesCodec.encodeResponse(WearEntriesResponse(entries = many)),
      )!!
    assertEquals(WEAR_ENTRIES_PAGE_SIZE, decoded.entries.size)
    assertEquals(50L, decoded.entries.first().id)
    assertTrue(decoded.entries.all { it.text.length <= WEAR_MAX_ENTRY_TEXT_LENGTH })
  }

  @Test
  fun deniedResponseNeverCarriesContent() {
    val leaky = WearEntriesResponse(entries = listOf(entry(1, text = "secret")), hasMore = true, denied = true)
    val raw = WearEntriesCodec.encodeResponse(leaky)
    assertTrue("secret" !in raw)
    val decoded = WearEntriesCodec.decodeResponse(raw)!!
    assertTrue(decoded.denied)
    assertTrue(decoded.entries.isEmpty())
  }

  @Test
  fun fullPageFitsInAMessage() {
    val full = (1L..WEAR_ENTRIES_PAGE_SIZE.toLong()).map { entry(it, text = "é".repeat(WEAR_MAX_ENTRY_TEXT_LENGTH)) }
    val bytes = WearEntriesCodec.encodeResponse(WearEntriesResponse(entries = full)).toByteArray().size
    assertTrue("$bytes bytes", bytes < 100_000)
  }
}

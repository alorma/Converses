package com.alorma.contactnotes.wear.data

import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearEntriesResponse
import com.alorma.contactnotes.shared.WearEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MoreEntriesSourceTest {
  private val entry = WearEntry(1, EntryKind.SAID, ChannelType.CHAT, "hola", 10)

  @Test
  fun pageKeepsEntriesAndHasMore() {
    val result = WearEntriesResponse(entries = listOf(entry), hasMore = true).toResult()
    assertEquals(MoreEntriesResult.Page(listOf(entry), hasMore = true), result)
  }

  @Test
  fun deniedOrUnreadableMeansUnavailable() {
    assertEquals(MoreEntriesResult.Unavailable, WearEntriesResponse(denied = true).toResult())
    assertEquals(MoreEntriesResult.Unavailable, (null as WearEntriesResponse?).toResult())
  }
}

package com.alorma.contactnotes.ui.people

import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeopleFeaturedTest {
  private fun item(
    name: String,
    at: Long?,
    protected: Boolean = false,
  ) = PersonListItem(
    person = PersonEntity(name = name, isProtected = protected, createdAt = 0, updatedAt = 0),
    lastEntry =
      at?.let {
        EntryEntity(personId = 1, kind = EntryKind.SAID, channelType = ChannelType.CHAT, text = "x", occurredAt = it)
      },
    channels = emptyList(),
    lastActivityAt = at ?: 0,
  )

  @Test
  fun picksTheMostRecentEntry() {
    val list = listOf(item("A", 1_000), item("B", 3_000), item("C", 2_000))
    assertEquals("B", list.featured()?.person?.name)
  }

  @Test
  fun skipsProtectedPeople() {
    val list = listOf(item("A", 1_000), item("B", 3_000, protected = true), item("C", 2_000))
    assertEquals("C", list.featured()?.person?.name)
  }

  @Test
  fun noneWhenOnlyProtectedOrWithoutEntries() {
    assertNull(listOf(item("A", 3_000, protected = true), item("B", null)).featured())
  }

  @Test
  fun placeholderHidesProtectedAndArchivedPeople() {
    fun person(
      protected: Boolean = false,
      archived: Boolean = false,
    ) = PersonEntity(name = "A", isProtected = protected, archived = archived, createdAt = 0, updatedAt = 0)
    assertEquals(false, person().hiddenInPlaceholder())
    assertEquals(true, person(protected = true).hiddenInPlaceholder())
    assertEquals(true, person(archived = true).hiddenInPlaceholder())
  }

  @Test
  fun emptyListHasNone() {
    assertNull(emptyList<PersonListItem>().featured())
  }

  @Test
  fun tieKeepsListOrder() {
    val list = listOf(item("A", 2_000), item("B", 2_000))
    assertEquals("A", list.featured()?.person?.name)
  }

  @Test
  fun justNowUnderAMinute() {
    assertNull(featuredTimeAt(10_000, 69_999))
  }

  @Test
  fun futureIsJustNow() {
    assertNull(featuredTimeAt(100_000, 10_000))
  }

  @Test
  fun olderKeepsTheTime() {
    assertEquals(10_000L, featuredTimeAt(10_000, 70_000))
  }
}

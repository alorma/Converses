package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.wear.components.EntryAuthor
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotMappingTest {
  private val zone = ZoneId.of("UTC")
  private val now = ZonedDateTime.of(2026, 10, 12, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

  private val labels =
    object : WearLabels {
      override val me = "Jo"
      override val note = "Apunt"

      override fun channel(type: ChannelType) = type.name

      override fun relative(time: RelativeTime) = time.toString()

      override fun clock(timestamp: Long) = "21:40"
    }

  private fun ago(minutes: Long) = now - minutes * 60_000L

  @Test
  fun relativeTimeBuckets() {
    assertEquals(RelativeTime.JustNow, relativeTime(ago(0), now, zone))
    assertEquals(RelativeTime.Minutes(14), relativeTime(ago(14), now, zone))
    assertEquals(RelativeTime.Hours(2), relativeTime(ago(125), now, zone))
    assertEquals(RelativeTime.Yesterday, relativeTime(ago(12 * 60 + 30), now, zone))
    assertEquals(RelativeTime.Date(ago(60 * 24 * 3)), relativeTime(ago(60 * 24 * 3), now, zone))
  }

  @Test
  fun futureTimestampIsJustNow() {
    assertEquals(RelativeTime.JustNow, relativeTime(now + 5 * 60_000L, now, zone))
  }

  @Test
  fun protectedPersonShowsNoLastEntry() {
    val person =
      WearPerson(1, "Marta", AvatarColor.Peach, AvatarShape.entries.first(), isProtected = true, lastEntryAt = ago(5))
    assertNull(person.toItem(labels, now, zone).lastEntry)
  }

  @Test
  fun normalPersonShowsRelativeLastEntry() {
    val person =
      WearPerson(1, "Kael", AvatarColor.Sky, AvatarShape.entries.first(), isProtected = false, lastEntryAt = ago(120))
    assertEquals("Hours(count=2)", person.toItem(labels, now, zone).lastEntry)
  }

  @Test
  fun entryAuthorsFollowWhoLabel() {
    val person = WearPerson(1, "Luna Roja", AvatarColor.Lavender, AvatarShape.entries.first(), isProtected = false)

    fun entry(kind: EntryKind) = WearEntry(9, kind, ChannelType.CHAT, "hola", now).toItem(person, labels)

    assertEquals(EntryAuthor.Me, entry(EntryKind.SAID).author)
    assertEquals("Jo · 21:40 · CHAT", entry(EntryKind.SAID).meta)
    assertEquals(EntryAuthor.Person("LR", AvatarColor.Lavender), entry(EntryKind.HEARD).author)
    assertEquals("Luna · 21:40 · CHAT", entry(EntryKind.HEARD).meta)
    assertEquals(EntryAuthor.Note, entry(EntryKind.HAPPENED).author)
    assertEquals("Apunt · 21:40 · CHAT", entry(EntryKind.HAPPENED).meta)
  }
}

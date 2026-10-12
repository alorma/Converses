package com.alorma.contactnotes.wear.data

import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedSessionsTest {
  private fun entry(id: Long) = WearEntry(id, EntryKind.SAID, ChannelType.CHAT, "e$id", 100 + id)

  private fun person(
    id: Long,
    isProtected: Boolean,
  ) = WearPerson(id, "P$id", AvatarColor.Sky, AvatarShape.entries.first(), isProtected, lastEntryAt = null)

  private val stored =
    StoredSnapshot(
      WearSnapshot(
        generatedAt = 0,
        appLockActive = false,
        people = listOf(person(1, true), person(2, true), person(3, false)),
      ),
      syncedAt = 0,
    )

  private fun session(personId: Long) =
    ProtectedSession(personId, listOf(entry(1), entry(2)), hasMore = false, "tok", 9)

  @Test
  fun anOpenSessionTurnsTheProtectedPersonIntoOneWithEntries() =
    runTest {
      val sessions = ProtectedSessions()
      sessions.open(session(1))

      val people =
        flowOf(stored)
          .withProtectedSessions(sessions.all)
          .first()!!
          .snapshot.people

      val opened = people.first { it.id == 1L }
      assertEquals(false, opened.isProtected)
      assertEquals(listOf(1L, 2L), opened.entries.map { it.id })
      assertEquals(102L, opened.lastEntryAt)
    }

  @Test
  fun otherProtectedPeopleStayClosed() =
    runTest {
      val sessions = ProtectedSessions()
      sessions.open(session(1))

      val other =
        flowOf(stored)
          .withProtectedSessions(sessions.all)
          .first()!!
          .snapshot.people
          .first { it.id == 2L }

      assertTrue(other.isProtected)
      assertTrue(other.entries.isEmpty())
    }

  @Test
  fun closingOrClearingForgetsTheEntries() =
    runTest {
      val sessions = ProtectedSessions()
      sessions.open(session(1))
      sessions.open(session(2))

      sessions.close(1)
      assertNull(sessions.get(1))
      assertEquals(2L, sessions.get(2)?.personId)

      sessions.clear()
      assertTrue(sessions.all.value.isEmpty())
    }

  @Test
  fun aNullSnapshotStaysNull() =
    runTest {
      assertNull(flowOf<StoredSnapshot?>(null).withProtectedSessions(ProtectedSessions().all).first())
    }
}

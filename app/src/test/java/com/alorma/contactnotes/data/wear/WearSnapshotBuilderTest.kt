package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.shared.WEAR_MAX_ENTRY_TEXT_LENGTH
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSnapshotBuilderTest {
  private val db = FakeDb()

  private suspend fun snapshot(appLockActive: Boolean = false): WearSnapshot =
    WearSnapshotBuilder.build(
      people = db.repository.observePeople().first(),
      recentEntries = db.repository.observeRecentEntriesForWear().first(),
      appLockActive = appLockActive,
      now = 5_000,
    )

  private fun WearSnapshot.person(name: String): WearPerson = people.single { it.name == name }

  @Test
  fun protectedPersonNeverCarriesContent() =
    runTest {
      val marta = db.addPerson("Marta", isProtected = true)
      db.addEntry(marta.id, text = "secret", occurredAt = 100)

      val result = snapshot().person("Marta")

      assertTrue(result.isProtected)
      assertTrue(result.entries.isEmpty())
      assertNull(result.lastEntryAt)
    }

  @Test
  fun protectedPersonStaysEmptyEvenIfTheInputsCarryContent() =
    runTest {
      val marta = db.addPerson("Marta", isProtected = true)
      val leaked = db.addEntry(marta.id, text = "secret", occurredAt = 100)

      val result =
        WearSnapshotBuilder
          .build(
            people = listOf(PersonListItem(marta, leaked, listOf(leaked.channelType))),
            recentEntries = listOf(leaked),
            appLockActive = false,
            now = 1,
          ).person("Marta")

      assertTrue(result.entries.isEmpty())
      assertNull(result.lastEntryAt)
    }

  @Test
  fun aPersonWhoBecomesProtectedLosesTheEntriesOnTheNextSnapshot() =
    runTest {
      val laia = db.addPerson("Laia")
      db.addEntry(laia.id, text = "hola", occurredAt = 100)
      assertEquals(1, snapshot().person("Laia").entries.size)

      db.people.value = db.people.value.map { if (it.id == laia.id) it.copy(isProtected = true) else it }

      val after = snapshot().person("Laia")
      assertTrue(after.isProtected)
      assertTrue(after.entries.isEmpty())
      assertNull(after.lastEntryAt)
    }

  @Test
  fun archivedPeopleAreNotIncluded() =
    runTest {
      db.addPerson("Visible")
      db.addPerson("Arxivada", archived = true)

      assertEquals(listOf("Visible"), snapshot().people.map { it.name })
    }

  @Test
  fun keepsTheLatestFiveEntriesNewestFirst() =
    runTest {
      val pau = db.addPerson("Pau")
      (1L..8L).forEach { db.addEntry(pau.id, text = "n$it", occurredAt = it * 10) }

      val result = snapshot().person("Pau")

      assertEquals(listOf("n8", "n7", "n6", "n5", "n4"), result.entries.map { it.text })
      assertEquals(80L, result.lastEntryAt)
    }

  @Test
  fun entriesAreKeptPerPerson() =
    runTest {
      val a = db.addPerson("A")
      val b = db.addPerson("B")
      repeat(7) { db.addEntry(a.id, text = "a$it", occurredAt = it.toLong()) }
      db.addEntry(b.id, text = "b", occurredAt = 1)

      val result = snapshot()

      assertEquals(5, result.person("A").entries.size)
      assertEquals(listOf("b"), result.person("B").entries.map { it.text })
    }

  @Test
  fun longEntryTextIsTruncated() =
    runTest {
      val ona = db.addPerson("Ona")
      db.addEntry(ona.id, text = "x".repeat(2_000), occurredAt = 1)

      assertEquals(
        WEAR_MAX_ENTRY_TEXT_LENGTH,
        snapshot()
          .person("Ona")
          .entries
          .single()
          .text.length,
      )
    }

  @Test
  fun personWithoutEntriesHasNoLastEntry() =
    runTest {
      db.addPerson("Nova")

      val nova = snapshot().person("Nova")

      assertNull(nova.lastEntryAt)
      assertTrue(nova.entries.isEmpty())
    }

  @Test
  fun appLockStateIsIncluded() =
    runTest {
      assertTrue(snapshot(appLockActive = true).appLockActive)
      assertFalse(snapshot(appLockActive = false).appLockActive)
    }

  @Test
  fun carriesTheAvatarAndTheGenerationTime() =
    runTest {
      val kael = db.addPerson("Kael")

      val result = snapshot()

      assertEquals(5_000L, result.generatedAt)
      assertEquals(kael.avatarColor, result.person("Kael").avatarColor)
      assertEquals(kael.avatarShape, result.person("Kael").avatarShape)
    }
}

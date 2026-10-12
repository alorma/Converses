package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.shared.WearSnapshotCodec
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearSnapshotPublisherTest {
  private class FakeWearLink : WearLink {
    val present = MutableStateFlow(true)
    override val watchPresent: Flow<Boolean> = present
    val payloads = mutableListOf<String>()
    var clears = 0
    var failNext = false

    override suspend fun clear() {
      clears++
    }

    override suspend fun publish(payload: ByteArray) {
      if (failNext) {
        failNext = false
        error("boom")
      }
      payloads += payload.toString(Charsets.UTF_8)
    }

    fun snapshots(): List<WearSnapshot> = payloads.map { WearSnapshotCodec.decode(it)!! }
  }

  private val db = FakeDb()
  private val settings = FakeSettingsRepository()
  private val link = FakeWearLink()
  private var now = 1_000L

  private fun TestScope.startPublisher() {
    WearSnapshotPublisher(
      repository = db.repository,
      settings = settings,
      link = link,
      scope = backgroundScope,
      clock = { now++ },
      debounceMillis = 500,
    ).start()
  }

  private fun TestScope.settle() {
    runCurrent()
    advanceTimeBy(600)
    runCurrent()
  }

  @Test
  fun publishesTheFirstSnapshotOnStart() =
    runTest {
      db.addPerson("Kael")

      startPublisher()
      settle()

      assertEquals(
        listOf("Kael"),
        link
          .snapshots()
          .single()
          .people
          .map { it.name },
      )
    }

  @Test
  fun publishesAgainWhenTheDataChanges() =
    runTest {
      val kael = db.addPerson("Kael")
      startPublisher()
      settle()

      db.addEntry(kael.id, text = "hola", occurredAt = 50)
      settle()

      assertEquals(2, link.payloads.size)
      assertEquals(
        "hola",
        link
          .snapshots()
          .last()
          .people
          .single()
          .entries
          .single()
          .text,
      )
    }

  @Test
  fun doesNotRepublishWhenNothingChanged() =
    runTest {
      db.addPerson("Kael")
      startPublisher()
      settle()

      // La mateixa configuració tornada a emetre no és un canvi
      settings.state.update { it.copy() }
      settle()

      assertEquals(1, link.payloads.size)
    }

  @Test
  fun coalescesRapidChangesIntoOnePublication() =
    runTest {
      val kael = db.addPerson("Kael")
      startPublisher()
      settle()

      repeat(5) { db.addEntry(kael.id, text = "n$it", occurredAt = it.toLong()) }
      settle()

      assertEquals(2, link.payloads.size)
    }

  @Test
  fun publishesNothingWhileThereIsNoWatch() =
    runTest {
      val kael = db.addPerson("Kael")
      link.present.value = false
      startPublisher()
      settle()

      db.addEntry(kael.id, text = "hola", occurredAt = 5)
      settle()

      assertTrue(link.payloads.isEmpty())
    }

  @Test
  fun publishesWhenAWatchAppearsEvenWithoutChanges() =
    runTest {
      db.addPerson("Kael")
      link.present.value = false
      startPublisher()
      settle()

      link.present.value = true
      settle()

      assertEquals(1, link.payloads.size)
    }

  @Test
  fun republishesWhenTheWatchComesBack() =
    runTest {
      db.addPerson("Kael")
      startPublisher()
      settle()

      link.present.value = false
      settle()
      link.present.value = true
      settle()

      assertEquals(2, link.payloads.size)
    }

  @Test
  fun clearsTheSnapshotWhenTheWatchGoesAway() =
    runTest {
      db.addPerson("Kael")
      startPublisher()
      settle()
      assertEquals(0, link.clears)

      link.present.value = false
      settle()

      assertEquals(1, link.clears)
    }

  @Test
  fun changesWhileThereIsNoWatchAreNotPublishedAfterwardsEither() =
    runTest {
      val laia = db.addPerson("Laia")
      db.addEntry(laia.id, text = "molt secret", occurredAt = 10)
      startPublisher()
      settle()
      link.present.value = false
      settle()

      db.people.update { list -> list.map { if (it.id == laia.id) it.copy(isProtected = true) else it } }
      settle()
      link.present.value = true
      settle()

      // La que es publica en tornar ja porta la protegida sense contingut
      assertFalse("molt secret" in link.payloads.last())
      assertTrue(
        link
          .snapshots()
          .last()
          .people
          .single()
          .isProtected,
      )
    }

  @Test
  fun appLockChangeIsPublished() =
    runTest {
      db.addPerson("Kael")
      startPublisher()
      settle()
      assertFalse(link.snapshots().last().appLockActive)

      settings.state.update { it.copy(appLock = true) }
      settle()

      assertTrue(link.snapshots().last().appLockActive)
    }

  @Test
  fun personWhoBecomesProtectedIsPublishedWithoutContent() =
    runTest {
      val laia = db.addPerson("Laia")
      db.addEntry(laia.id, text = "molt secret", occurredAt = 10)
      startPublisher()
      settle()
      assertTrue("molt secret" in link.payloads.last())

      db.people.update { list -> list.map { if (it.id == laia.id) it.copy(isProtected = true) else it } }
      settle()

      assertFalse("molt secret" in link.payloads.last())
      assertTrue(
        link
          .snapshots()
          .last()
          .people
          .single()
          .isProtected,
      )
    }

  @Test
  fun aFailedPublicationDoesNotStopTheNextOnes() =
    runTest {
      val kael = db.addPerson("Kael")
      link.failNext = true
      startPublisher()
      settle()
      assertTrue(link.payloads.isEmpty())

      db.addEntry(kael.id, text = "hola", occurredAt = 5)
      settle()

      assertEquals(1, link.payloads.size)
    }
}

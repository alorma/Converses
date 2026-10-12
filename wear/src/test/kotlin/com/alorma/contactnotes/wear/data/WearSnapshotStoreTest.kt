package com.alorma.contactnotes.wear.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.shared.WearSnapshotCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WearSnapshotStoreTest {
  @get:Rule
  val folder = TemporaryFolder()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var now = 7_000L

  private val dataStore by lazy {
    PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("snapshot.preferences_pb") }
  }
  private var deviceSecure = true
  private val store by lazy { WearSnapshotStore(dataStore, { deviceSecure }, clock = { now }) }

  @After
  fun tearDown() = scope.cancel()

  private fun entry(id: Long) = WearEntry(id, EntryKind.SAID, ChannelType.CHAT, "text $id", id)

  private fun person(
    id: Long,
    isProtected: Boolean = false,
    entries: List<WearEntry> = emptyList(),
  ) = WearPerson(
    id,
    "P$id",
    AvatarColor.Mint,
    AvatarShape.Sunny,
    isProtected,
    entries.maxOfOrNull {
      it.occurredAt
    },
    entries,
  )

  private fun payload(vararg people: WearPerson) =
    WearSnapshotCodec.encode(WearSnapshot(generatedAt = 1, appLockActive = true, people = people.toList()))

  @Test
  fun startsEmpty() =
    runTest {
      assertNull(store.stored.first())
    }

  @Test
  fun savesTheSnapshotWithTheSyncTime() =
    runTest {
      assertTrue(store.save(payload(person(1, entries = listOf(entry(1))))))

      val stored = store.stored.first()!!
      assertEquals(7_000L, stored.syncedAt)
      assertEquals(listOf("P1"), stored.snapshot.people.map { it.name })
      assertEquals(
        1,
        stored.snapshot.people
          .single()
          .entries.size,
      )
    }

  @Test
  fun aNewSnapshotReplacesTheOldOne() =
    runTest {
      store.save(payload(person(1)))
      now = 9_000
      store.save(payload(person(2)))

      val stored = store.stored.first()!!
      assertEquals(listOf("P2"), stored.snapshot.people.map { it.name })
      assertEquals(9_000L, stored.syncedAt)
    }

  @Test
  fun aProtectedPersonWithContentIsNeverStored() =
    runTest {
      val leaky =
        """{"version":1,"generatedAt":1,"appLockActive":false,"people":[""" +
          """{"id":1,"name":"A","avatarColor":"Sky","avatarShape":"Sunny","isProtected":true,"lastEntryAt":7,""" +
          """"entries":[{"id":1,"kind":"SAID","channel":"CHAT","text":"molt secret","occurredAt":7}]}]}"""

      assertTrue(store.save(leaky))

      val person =
        store.stored
          .first()!!
          .snapshot.people
          .single()
      assertTrue(person.entries.isEmpty())
      assertNull(person.lastEntryAt)
      val raw =
        dataStore.data
          .first()
          .asMap()
          .values
          .joinToString()
      assertFalse("molt secret" in raw)
    }

  @Test
  fun aPersonWhoBecomesProtectedLosesTheEntriesOnTheNextSync() =
    runTest {
      store.save(payload(person(1, entries = listOf(entry(1)))))
      store.save(payload(person(1, isProtected = true)))

      val person =
        store.stored
          .first()!!
          .snapshot.people
          .single()
      assertTrue(person.isProtected)
      assertTrue(person.entries.isEmpty())
    }

  @Test
  fun anUnreadablePayloadKeepsThePreviousCopy() =
    runTest {
      store.save(payload(person(1)))

      assertFalse(store.save("not json"))
      assertFalse(store.save("""{"version":99,"generatedAt":1,"appLockActive":true,"people":[]}"""))

      assertEquals(
        listOf("P1"),
        store.stored
          .first()!!
          .snapshot.people
          .map { it.name },
      )
    }

  @Test
  fun withTheAppLockOnAndAnInsecureWatchNothingIsSavedAndTheCopyIsErased() =
    runTest {
      store.save(payload(person(1, entries = listOf(entry(1)))))
      deviceSecure = false

      assertFalse(store.save(payload(person(2))))

      assertNull(store.stored.first())
    }

  @Test
  fun withTheAppLockOffAnInsecureWatchStillSaves() =
    runTest {
      deviceSecure = false
      val off =
        WearSnapshotCodec.encode(
          WearSnapshot(generatedAt = 1, appLockActive = false, people = listOf(person(1))),
        )

      assertTrue(store.save(off))

      assertEquals(
        listOf("P1"),
        store.stored
          .first()!!
          .snapshot.people
          .map { it.name },
      )
    }

  @Test
  fun clearRemovesTheCopy() =
    runTest {
      store.save(payload(person(1)))
      store.clear()
      assertNull(store.stored.first())
    }
}

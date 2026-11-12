package com.alorma.contactnotes.data.sync.engine

import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.sync.RemoteRecord
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SyncEngine
import com.alorma.contactnotes.data.sync.SyncOutcome
import com.alorma.contactnotes.data.sync.SyncRemote
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.record.RecordCodec
import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.testing.FakeCursorStore
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.FakeSyncRemote
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val UID = "uid-1"

/** Un remot que deixa posar-hi un ganxo a la pujada i fer fallar pujades o baixades a partir d'un nombre de crida. */
private class HookedRemote(
  val inner: FakeSyncRemote,
) : SyncRemote by inner {
  var onPush: suspend () -> Unit = {}
  var failPushFrom: Int? = null
  var failPullFrom: Int? = null
  private var pushCalls = 0
  private var pullCalls = 0

  override suspend fun push(
    uid: String,
    records: List<RemoteRecord>,
  ): RemoteResult<Unit> {
    pushCalls++
    if (failPushFrom?.let { pushCalls >= it } == true) return RemoteResult.Failed
    val result = inner.push(uid, records)
    onPush()
    return result
  }

  override suspend fun pull(
    uid: String,
    since: Long,
    limit: Int,
  ): RemoteResult<List<RemoteRecord>> {
    pullCalls++
    if (failPullFrom?.let { pullCalls >= it } == true) return RemoteResult.Failed
    return inner.pull(uid, since, limit)
  }
}

/** Un dispositiu: la seva base de dades i el seu cursor, amb el mateix compte i la mateixa clau que els altres. */
private class Device(
  deviceId: String,
  private val remote: SyncRemote,
  private val pageSize: Int = 200,
  val db: FakeDb = FakeDb(),
  val cursors: FakeCursorStore = FakeCursorStore(deviceId = deviceId),
) {
  val errors = mutableListOf<String>()
  val cipher = RecordCipher(DataKey(ByteArray(32) { it.toByte() }), UID)
  var engine = newEngine()

  fun newEngine() =
    SyncEngine(db.syncSource, remote, cursors, clock = { 100_000L }, onError = {
      _,
      where,
      ->
      errors += where
    }, pageSize)

  suspend fun push() = engine.pushPending(UID, cipher)

  suspend fun pull() = engine.pullAll(UID, cipher)

  fun edit(
    syncId: String,
    name: String,
    at: Long,
  ) = db.people.value
    .first { it.syncId == syncId }
    .let { p ->
      db.people.value =
        db.people.value.map { if (it.syncId == syncId) p.copy(name = name, updatedAt = at, dirty = true) else it }
    }

  val names get() = db.people.value.map { it.name }
}

class SyncEngineTest {
  private val server = FakeSyncRemote()
  private val remote = HookedRemote(server)
  private val a = Device("device-a", remote)
  private val b = Device("device-b", remote)

  /** Una persona que els dos dispositius ja tenen, neta. */
  private suspend fun sharedPerson(): String {
    val person = a.db.addPerson("Ana")
    a.push()
    b.pull()
    return person.syncId
  }

  @Test
  fun everyTypeSurvivesTheRoundTrip() =
    runTest {
      val channel = a.db.addCustomChannel("Signal")
      val person = a.db.addPerson("Ana", alias = "A", isProtected = true)
      a.db.facts.value = listOf(PinnedFactEntity(personId = person.id, label = "Dog", value = "Rex", position = 0))
      a.db.addEntry(person.id, text = "hello", customChannelId = channel.id)
      a.db.addTask(person.id, text = "call")

      assertEquals(SyncOutcome.Done, a.push())
      assertEquals(SyncOutcome.Done, b.pull())

      val p =
        b.db.people.value
          .single()
      assertEquals("Ana", p.name)
      assertEquals("A", p.alias)
      assertTrue(p.isProtected)
      assertEquals(person.syncId, p.syncId)
      assertEquals(
        "Rex",
        b.db.facts.value
          .single()
          .value,
      )
      assertEquals(
        b.db.channels.value
          .single()
          .id,
        b.db.entries.value
          .single()
          .customChannelId,
      )
      assertEquals(
        "hello",
        b.db.entries.value
          .single()
          .text,
      )
      assertEquals(
        "call",
        b.db.tasks.value
          .single()
          .text,
      )
      assertTrue(b.errors.isEmpty())
    }

  @Test
  fun pushingClearsDirtyAndUploadedTombstones() =
    runTest {
      val person = a.db.addPerson("Ana")
      val gone = a.db.addPerson("Bea")
      a.push()
      a.db.repository.deletePerson(gone)
      assertTrue(
        a.db.tombstones.value
          .isNotEmpty(),
      )

      a.push()

      assertTrue(
        a.db.syncSource
          .readPending()
          .isEmpty,
      )
      assertTrue(
        a.db.tombstones.value
          .isEmpty(),
      )
      assertTrue(server.records.getValue(gone.syncId).deleted)
      assertFalse(server.records.getValue(person.syncId).deleted)
    }

  @Test
  fun anEditDuringThePushStaysDirtyAndGoesUpNextTime() =
    runTest {
      val person = a.db.addPerson("Ana")
      remote.onPush = {
        a.edit(person.syncId, "Anna", at = 5_000)
        remote.onPush = {}
      }

      a.push()
      assertTrue(
        a.db.people.value
          .single()
          .dirty,
      )

      a.push()
      assertTrue(
        a.db.syncSource
          .readPending()
          .isEmpty,
      )
      b.pull()
      assertEquals(listOf("Anna"), b.names)
    }

  @Test
  fun aDownloadedChangeDoesNotBecomeDirty() =
    runTest {
      val person = a.db.addPerson("Ana")
      a.db.addEntry(person.id)
      a.push()

      b.pull()

      assertTrue(
        b.db.syncSource
          .readPending()
          .isEmpty,
      )
      assertTrue(
        b.db.tombstones.value
          .isEmpty(),
      )
    }

  @Test
  fun theNewerEditWinsOnBothDevices() =
    runTest {
      val id = sharedPerson()
      a.edit(id, "from-a", at = 2_000)
      a.push()
      b.edit(id, "from-b", at = 3_000)

      b.pull()
      assertEquals(listOf("from-b"), b.names)
      b.push()
      a.pull()

      assertEquals(listOf("from-b"), a.names)
    }

  @Test
  fun anOlderRemoteEditDoesNotOverwriteANewerLocalOne() =
    runTest {
      val id = sharedPerson()
      b.edit(id, "from-b", at = 2_000)
      b.push()
      a.edit(id, "from-a", at = 3_000)

      a.pull()

      assertEquals(listOf("from-a"), a.names)
    }

  @Test
  fun aTieGoesToTheLargerDeviceId() =
    runTest {
      val id = sharedPerson()
      a.edit(id, "from-a", at = 2_000)
      a.push()
      b.edit(id, "from-b", at = 2_000)

      // b (device-b) és més gran: guanya la seva
      b.pull()
      assertEquals(listOf("from-b"), b.names)
      b.push()
      a.pull()
      assertEquals(listOf("from-b"), a.names)
    }

  @Test
  fun aTieGoesToTheRemoteWhenItsDeviceIdIsLarger() =
    runTest {
      val id = sharedPerson()
      b.edit(id, "from-b", at = 2_000)
      b.push()
      a.edit(id, "from-a", at = 2_000)

      a.pull()

      assertEquals(listOf("from-b"), a.names)
    }

  @Test
  fun aDeletionWinsOverAnEarlierEdit() =
    runTest {
      val id = sharedPerson()
      b.edit(id, "from-b", at = 2_000)
      a.db.clock = { 5_000 }
      a.db.repository.deletePerson(
        a.db.people.value
          .single(),
      )
      a.push()

      b.pull()

      assertTrue(
        b.db.people.value
          .isEmpty(),
      )
    }

  @Test
  fun aDeletionLosesAgainstALaterEdit() =
    runTest {
      val id = sharedPerson()
      b.edit(id, "from-b", at = 9_000)
      a.db.clock = { 5_000 }
      a.db.repository.deletePerson(
        a.db.people.value
          .single(),
      )
      a.push()

      b.pull()
      assertEquals(listOf("from-b"), b.names)
      b.push()
      a.pull()

      assertEquals(listOf("from-b"), a.names)
    }

  @Test
  fun aLocalDeletionOnlyGivesWayToALaterRemoteEdit() =
    runTest {
      val id = sharedPerson()
      a.db.clock = { 5_000 }
      a.db.repository.deletePerson(
        a.db.people.value
          .single(),
      )
      b.edit(id, "old", at = 3_000)
      b.push()

      a.pull()
      assertTrue(
        a.db.people.value
          .isEmpty(),
      )
      assertEquals(1, a.db.tombstones.value.size)

      b.edit(id, "new", at = 7_000)
      b.push()
      a.pull()

      assertEquals(listOf("new"), a.names)
      assertTrue(
        a.db.tombstones.value
          .isEmpty(),
      )
    }

  @Test
  fun entriesBeforeTheirPersonInTheSamePageAreApplied() =
    runTest {
      val person = a.db.addPerson("Ana")
      a.db.addEntry(person.id)
      a.db.addTask(person.id)
      a.push()
      server.shuffled = true

      b.pull()

      assertEquals(1, b.db.entries.value.size)
      assertEquals(1, b.db.tasks.value.size)
    }

  @Test
  fun anEntryInAnEarlierPageWaitsForItsPerson() =
    runTest {
      val person = a.db.addPerson("Ana")
      a.db.addEntry(person.id)
      a.push()
      a.edit(person.syncId, "Anna", at = 2_000)
      a.push()
      val small = Device("device-c", remote, pageSize = 1)

      small.pull()

      assertEquals(1, small.db.entries.value.size)
      assertEquals(listOf("Anna"), small.names)
      assertEquals(
        server.records.values
          .maxOf { it.updatedAt },
        small.cursors.cursor,
      )
    }

  @Test
  fun aDeadProcessMidPullLosesNothingAndLeavesNoOrphans() =
    runTest {
      val person = a.db.addPerson("Ana")
      a.db.addEntry(person.id)
      a.push()
      a.edit(person.syncId, "Anna", at = 2_000)
      a.push()
      val entryUpdatedAt = server.records.values.minOf { it.updatedAt }
      val c = Device("device-c", remote, pageSize = 1)
      remote.failPullFrom = 2

      assertEquals(SyncOutcome.Failed, c.pull())
      // L'entrada espera la persona: el cursor es queda abans d'ella
      assertEquals(entryUpdatedAt - 1, c.cursors.cursor)
      assertTrue(
        c.db.entries.value
          .isEmpty(),
      )

      remote.failPullFrom = null
      val restarted = c.newEngine()
      assertEquals(SyncOutcome.Done, restarted.pullAll(UID, c.cipher))

      assertEquals(1, c.db.entries.value.size)
      assertEquals(listOf("Anna"), c.names)
    }

  @Test
  fun anEntryWhoseParentWasDeletedIsDroppedAtTheEnd() =
    runTest {
      val person = a.db.addPerson("Ana")
      a.db.addEntry(person.id)
      a.push()
      remote.inner.push(
        UID,
        listOf(
          RemoteRecord(person.syncId, a.cipher.encrypt(person.syncId, RecordCodec.encodeDeleted(5_000)), true, "x", 0),
        ),
      )

      assertEquals(SyncOutcome.Done, b.pull())

      assertTrue(
        b.db.entries.value
          .isEmpty(),
      )
      assertEquals(
        server.records.values
          .maxOf { it.updatedAt },
        b.cursors.cursor,
      )
    }

  @Test
  fun customChannelsCreatedOnBothDevicesBecomeOne() =
    runTest {
      sharedPerson()
      val channelA = a.db.addCustomChannel("Signal")
      a.db.addEntry(
        a.db.people.value
          .single()
          .id,
        text = "from a",
        customChannelId = channelA.id,
      )
      val channelB = b.db.addCustomChannel("Signal")
      b.db.addEntry(
        b.db.people.value
          .single()
          .id,
        text = "from b",
        customChannelId = channelB.id,
      )

      repeat(3) {
        a.push()
        b.push()
        a.pull()
        b.pull()
      }

      assertEquals(1, a.db.channels.value.size)
      assertEquals(1, b.db.channels.value.size)
      assertEquals(
        a.db.channels.value
          .single()
          .syncId,
        b.db.channels.value
          .single()
          .syncId,
      )
      assertEquals(
        minOf(channelA.syncId, channelB.syncId),
        a.db.channels.value
          .single()
          .syncId,
      )
      listOf(a, b).forEach { device ->
        assertEquals(2, device.db.entries.value.size)
        val channelId =
          device.db.channels.value
            .single()
            .id
        assertTrue(
          device.db.entries.value
            .all { it.customChannelId == channelId },
        )
      }
    }

  @Test
  fun aProtectedPersonStaysProtected() =
    runTest {
      a.db.addPerson("Ana", isProtected = true)
      a.push()

      b.pull()

      assertTrue(
        b.db.people.value
          .single()
          .isProtected,
      )
    }

  @Test
  fun offlineAndFailedOutcomesAreReportedAndNothingIsLost() =
    runTest {
      a.db.addPerson("Ana")

      server.failure = RemoteResult.Offline
      assertEquals(SyncOutcome.Offline, a.push())
      assertEquals(SyncOutcome.Offline, a.pull())
      server.failure = RemoteResult.Failed
      assertEquals(SyncOutcome.Failed, a.push())
      assertEquals(SyncOutcome.Failed, a.pull())

      assertEquals(
        1,
        a.db.syncSource
          .readPending()
          .records.size,
      )
      assertEquals(0L, a.cursors.cursor)
    }

  @Test
  fun pushGoesInBatchesOf200AndStopsAtTheFirstFailure() =
    runTest {
      val person = a.db.addPerson("Ana")
      repeat(449) { a.db.addEntry(person.id) }

      a.push()
      assertEquals(3, server.pushes)

      val c = Device("device-c", remote)
      val p = c.db.addPerson("Bea")
      repeat(449) { c.db.addEntry(p.id) }
      remote.failPushFrom = 5 // la pujada d'abans ja n'ha fet 3

      assertEquals(SyncOutcome.Failed, c.push())
      assertEquals(
        250,
        c.db.syncSource
          .readPending()
          .records.size,
      )
    }

  @Test
  fun noRemoteRecordContainsPlaintext() =
    runTest {
      val person = a.db.addPerson("Ana Maria", alias = "Nini")
      a.db.addEntry(person.id, text = "secret text")
      a.db.addTask(person.id, text = "secret task")
      a.db.repository.deletePerson(a.db.addPerson("Bea Gone"))
      a.push()

      assertTrue(server.records.size >= 3)
      server.records.values.forEach { r ->
        val all = String(r.sealed, Charsets.ISO_8859_1) + r.id + r.by
        listOf("Ana", "Maria", "Nini", "secret", "Bea", "Gone").forEach { assertFalse(all.contains(it)) }
      }
    }

  @Test
  fun anUnreadableRecordIsSkippedAndCounted() =
    runTest {
      a.db.addPerson("Ana")
      a.push()
      remote.inner.push(UID, listOf(RemoteRecord("junk", byteArrayOf(1, 2, 3), false, "z", 0)))

      assertEquals(SyncOutcome.Done, b.pull())

      assertEquals(listOf("Ana"), b.names)
      assertEquals(listOf(ErrorSites.SYNC_RECORD), b.errors)
    }

  @Test
  fun ourOwnEchoDoesNotOverwriteANewerLocalEdit() =
    runTest {
      val person = a.db.addPerson("Ana")
      a.push()
      a.edit(person.syncId, "newer", at = 5_000)

      a.pull()

      assertEquals(listOf("newer"), a.names)
      assertTrue(
        a.db.people.value
          .single()
          .dirty,
      )
    }
}

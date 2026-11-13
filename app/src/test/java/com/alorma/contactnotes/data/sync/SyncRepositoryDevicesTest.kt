package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.record.SyncRecord
import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeAuthGateway
import com.alorma.contactnotes.testing.FakeCursorStore
import com.alorma.contactnotes.testing.FakeDeviceStore
import com.alorma.contactnotes.testing.FakeSyncKeyStore
import com.alorma.contactnotes.testing.FakeSyncLocalStore
import com.alorma.contactnotes.testing.FakeSyncRemote
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class DevRunner : SyncRunner {
  val calls = mutableListOf<String>()
  var pushOutcome = SyncOutcome.Done

  override suspend fun push(
    uid: String,
    cipher: RecordCipher,
  ): SyncOutcome {
    calls += "push"
    return pushOutcome
  }

  override suspend fun pull(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit,
  ): SyncOutcome {
    calls += "pull"
    return SyncOutcome.Done
  }
}

private class DevSource : LocalSyncSource {
  val pending = MutableStateFlow(0)

  override suspend fun entryCount(): Int = 0

  override fun observePendingCount(): Flow<Int> = pending

  override suspend fun readPending(): PendingChanges = error("not used")

  override suspend fun markClean(
    syncId: String,
    type: RecordType,
    modifiedAt: Long,
  ) = error("not used")

  override suspend fun markDirty(
    syncId: String,
    type: RecordType,
  ) = error("not used")

  override suspend fun markDeletionUploaded(
    syncId: String,
    deletedAt: Long,
  ) = error("not used")

  override suspend fun localState(syncId: String): LocalState = error("not used")

  override suspend fun applyRemote(
    syncId: String,
    record: SyncRecord,
  ): ApplyResult = error("not used")

  override suspend fun applyRemoteDeletion(
    syncId: String,
    type: RecordType,
  ) = error("not used")

  override suspend fun addTombstone(
    syncId: String,
    deletedAt: Long,
  ) = error("not used")

  override suspend fun ownChannels(): List<ChannelInfo> = error("not used")

  override suspend fun adoptChannelSyncId(
    localSyncId: String,
    remoteSyncId: String,
    now: Long,
  ) = error("not used")
}

/** Un servidor que fa veure que les regles han vist un estat més antic quan el dispositiu llegeix. */
private class StaleRemote(
  private val delegate: SyncRemote,
) : SyncRemote by delegate {
  var stale: RemoteDevices? = null

  override suspend fun loadDevices(uid: String): RemoteResult<RemoteDevices> =
    stale?.let { RemoteResult.Ok(it) } ?: delegate.loadDevices(uid)

  override suspend fun joinDevice(
    uid: String,
    device: RemoteDevice,
    removing: String?,
  ): JoinResult {
    stale = null
    return delegate.joinDevice(uid, device, removing)
  }
}

/** Esborrar mai respon. */
private class HangingRemote(
  private val delegate: SyncRemote,
) : SyncRemote by delegate {
  override suspend fun removeDevice(
    uid: String,
    id: String,
  ): RemoteResult<Unit> = awaitCancellation()
}

private const val PRO_DEVICES = 5
private const val FREE_DEVICES = 2

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryDevicesTest {
  private val accounts = FakeAccountRemote()
  private val fake = FakeSyncRemote()
  private var now = 1_000_000L
  private val activity = Activity()

  /** Un dispositiu: el seu repositori i els seus falsos. El compte i Firestore són compartits. */
  private inner class Device(
    scope: CoroutineScope,
    val deviceId: String,
    pro: Boolean = false,
    registered: Boolean = false,
    remote: SyncRemote = fake,
    val auth: FakeAuthGateway = FakeAuthGateway(),
    val keyStore: FakeSyncKeyStore = FakeSyncKeyStore(),
    val local: FakeSyncLocalStore = FakeSyncLocalStore(),
    val store: FakeDeviceStore = FakeDeviceStore(deviceId, registered),
  ) {
    val runner = DevRunner()
    val proFlow = MutableStateFlow(pro)
    val errors = mutableListOf<String>()
    val repo =
      SyncRepository(
        auth,
        accounts,
        keyStore,
        local,
        scope,
        clock = { now },
        sync =
          SyncData(
            runner,
            DevSource(),
            remote,
            FakeCursorStore(deviceId = deviceId),
            store,
            deviceName = { "Pixel" },
            isPro = { proFlow.value },
            isProChanges = proFlow,
            onError = { _, where -> errors += where },
          ),
      )

    suspend fun activate() {
      repo.signIn(activity)
      repo.confirmActivation()
    }

    fun words(): List<String> {
      repo.unlockWords()
      return repo.securityWords()!!
    }

    suspend fun enter(words: List<String>): WordsOutcome {
      repo.signIn(activity)
      return repo.submitWords(words)
    }

    /** Un dispositiu del compte que escriu un nom xifrat amb la clau d'aquest. */
    suspend fun seed(
      id: String,
      name: String = "Other",
    ) {
      val cipher = RecordCipher(keyStore.stored!!.dataKey, TestAccount.uid)
      val sealed = cipher.encrypt("device-name:$id", name.toByteArray())
      fake.joinDevice(TestAccount.uid, RemoteDevice(id, sealed, fake.pro), removing = null)
    }
  }

  private fun TestScope.device(
    id: String,
    pro: Boolean = false,
  ) = Device(backgroundScope, id, pro)

  private fun test(block: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher(), testBody = block)

  @Test
  fun joiningWithRoomAddsTheDevice() =
    test {
      val a = device("device-1")
      a.activate()
      val b = device("device-2")
      assertEquals(WordsOutcome.Accepted, b.enter(a.words()))
      assertEquals(listOf("device-1", "device-2"), fake.deviceIds)
      assertNull(b.repo.slotsPrompt.value)
      assertTrue(b.store.registered)
      assertTrue(b.repo.state.value is SyncState.Ready)
    }

  @Test
  fun joiningFullAsksWhichOneToRemoveAndDoesNotPreselectOrWrite() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1", "Tablet")
      val b = device("device-3")
      b.enter(a.words())
      val prompt = b.repo.slotsPrompt.value
      assertNotNull(prompt)
      assertEquals(FREE_DEVICES, prompt!!.limit)
      assertTrue(prompt.canUpgrade)
      assertEquals(setOf("Pixel", "Tablet"), prompt.devices.map { it.name }.toSet())
      assertFalse(prompt.devices.any { it.isThis })
      assertEquals(listOf("device-1", "other-1"), fake.deviceIds)
      assertTrue(b.runner.calls.isEmpty())
      assertNull(b.repo.initialSync.value)
      assertFalse(b.store.registered)
    }

  @Test
  fun choosingADeviceRemovesItAndJoinsInOneStepThenDownloads() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1")
      val b = device("device-3")
      b.enter(a.words())
      assertEquals(DeviceActionOutcome.Done, b.repo.chooseDeviceToRemove("other-1"))
      assertEquals(listOf("device-1", "device-3"), fake.deviceIds)
      assertNull(b.repo.slotsPrompt.value)
      assertTrue(b.store.registered)
      assertEquals(listOf("pull"), b.runner.calls)
      assertEquals(InitialSync.Done, b.repo.initialSync.value)
    }

  @Test
  fun choosingADeviceThatIsNotInThePromptDoesNothing() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1")
      val b = device("device-3")
      b.enter(a.words())
      assertEquals(DeviceActionOutcome.Failed, b.repo.chooseDeviceToRemove("nobody"))
      assertEquals(listOf("device-1", "other-1"), fake.deviceIds)
    }

  @Test
  fun notNowSignsOutAndLeavesNothingInTheCloud() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1")
      val b = device("device-3")
      b.enter(a.words())
      b.repo.cancelSlots()
      assertEquals(SyncState.SignedOut, b.repo.state.value)
      assertNull(b.repo.slotsPrompt.value)
      assertEquals(listOf("device-1", "other-1"), fake.deviceIds)
      assertFalse(b.store.registered)
      assertNull(b.keyStore.stored)
    }

  @Test
  fun theSecondOfTwoConcurrentJoinsGetsFullFromTheRulesAndSeesThePrompt() =
    test {
      val a = device("device-1")
      a.activate()
      val stale = StaleRemote(fake)
      val c = Device(backgroundScope, "device-3", remote = stale)
      val words = a.words()
      // C ha llegit quan només hi havia un dispositiu; mentrestant un altre ha ocupat la segona plaça
      stale.stale = (fake.loadDevices(TestAccount.uid) as RemoteResult.Ok).value
      a.seed("device-2")
      c.enter(words)
      assertNotNull(c.repo.slotsPrompt.value)
      assertEquals(listOf("device-1", "device-2"), fake.deviceIds)
      assertFalse(c.store.registered)
    }

  @Test
  fun removedWhileVisibleStopsTheEngineAndKeepsTheKey() =
    test {
      val a = device("device-1")
      a.activate()
      backgroundScope.launch { a.repo.watchDevices() }
      fake.removeFromOutside("device-1")
      assertTrue(a.repo.state.value is SyncState.Removed)
      a.runner.calls.clear()
      assertEquals(SyncOutcome.Done, a.repo.push())
      assertEquals(SyncOutcome.Done, a.repo.pull())
      assertTrue(a.runner.calls.isEmpty())
      assertNotNull(a.keyStore.stored)
      assertTrue(a.local.session)
      assertTrue(a.store.registered)
      assertTrue(fake.deviceIds.isEmpty())
    }

  @Test
  fun removedWithTheAppClosedIsSeenOnTheFirstSyncAndNeverReAdds() =
    test {
      val a = device("device-1")
      a.activate()
      fake.removeFromOutside("device-1")
      val fresh =
        Device(
          backgroundScope,
          "device-1",
          registered = true,
          auth = a.auth,
          keyStore = a.keyStore,
          local = a.local,
          store = a.store,
        )
      fresh.repo.start()
      assertTrue(fresh.repo.state.value is SyncState.Removed)
      fresh.repo.syncNow()
      assertTrue(fresh.runner.calls.isEmpty())
      assertTrue(fake.deviceIds.isEmpty())
    }

  @Test
  fun aRejectedPushThatLeavesTheDeviceInPlaceIsAnOrdinaryFailure() =
    test {
      val a = device("device-1")
      a.activate()
      a.runner.pushOutcome = SyncOutcome.Denied
      assertEquals(SyncOutcome.Failed, a.repo.push())
      assertTrue(a.repo.state.value is SyncState.Ready)
    }

  @Test
  fun aRejectedPushOfARemovedDeviceMakesItRemoved() =
    test {
      val a = device("device-1")
      a.activate()
      fake.removeFromOutside("device-1")
      a.runner.pushOutcome = SyncOutcome.Denied
      a.repo.push()
      assertTrue(a.repo.state.value is SyncState.Removed)
      assertTrue(fake.deviceIds.isEmpty())
    }

  @Test
  fun rejoiningWithRoomJoinsAndSyncs() =
    test {
      val a = device("device-1")
      a.activate()
      fake.removeFromOutside("device-1")
      forceRemoved(a)
      a.runner.calls.clear()
      assertEquals(RejoinOutcome.Done, a.repo.rejoin())
      assertTrue(a.repo.state.value is SyncState.Ready)
      assertEquals(listOf("device-1"), fake.deviceIds)
      assertEquals(listOf("push", "pull"), a.runner.calls)
    }

  private suspend fun forceRemoved(device: Device) {
    if (device.repo.state.value !is SyncState.Removed) {
      device.runner.pushOutcome = SyncOutcome.Denied
      device.repo.push()
      device.runner.pushOutcome = SyncOutcome.Done
    }
    assertTrue(device.repo.state.value is SyncState.Removed)
  }

  @Test
  fun rejoiningFullShowsThePromptAndChoosingResumes() =
    test {
      val a = device("device-1")
      a.activate()
      fake.removeFromOutside("device-1")
      forceRemoved(a)
      a.seed("other-1")
      a.seed("other-2")
      assertEquals(RejoinOutcome.Full, a.repo.rejoin())
      assertTrue(a.repo.state.value is SyncState.Removed)
      assertNotNull(a.repo.slotsPrompt.value)
      assertEquals(DeviceActionOutcome.Done, a.repo.chooseDeviceToRemove("other-1"))
      assertTrue(a.repo.state.value is SyncState.Ready)
      assertEquals(listOf("other-2", "device-1"), fake.deviceIds)
    }

  @Test
  fun rejoiningOfflineReportsOffline() =
    test {
      val a = device("device-1")
      a.activate()
      fake.removeFromOutside("device-1")
      forceRemoved(a)
      fake.failure = RemoteResult.Offline
      assertEquals(RejoinOutcome.Offline, a.repo.rejoin())
      assertTrue(a.repo.state.value is SyncState.Removed)
    }

  @Test
  fun signingOutOnlineFreesTheSlotAndKeepsTheDeviceId() =
    test {
      val a = device("device-1")
      a.activate()
      val words = a.words()
      a.repo.signOut()
      assertEquals(SyncState.SignedOut, a.repo.state.value)
      assertTrue(fake.deviceIds.isEmpty())
      assertFalse(a.store.registered)
      assertEquals("device-1", a.store.deviceId())
      a.enter(words)
      assertEquals(listOf("device-1"), fake.deviceIds)
      assertTrue(a.store.registered)
    }

  @Test
  fun signingOutOfflineSignsOutAnywayAndReEnteringReusesTheEntry() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1")
      val words = a.words()
      fake.failure = RemoteResult.Offline
      a.repo.signOut()
      assertEquals(SyncState.SignedOut, a.repo.state.value)
      assertFalse(a.store.registered)
      fake.failure = null
      assertEquals(listOf("device-1", "other-1"), fake.deviceIds)
      // Plaça plena, però aquest dispositiu ja hi és: no veu les places plenes
      a.enter(words)
      assertTrue(a.repo.state.value is SyncState.Ready)
      assertNull(a.repo.slotsPrompt.value)
      assertEquals(listOf("device-1", "other-1"), fake.deviceIds)
      assertTrue(a.store.registered)
    }

  @Test
  fun signingOutGivesUpWhenTheServerDoesNotAnswerIn20Seconds() =
    test {
      val a = Device(backgroundScope, "device-1", remote = HangingRemote(fake))
      a.activate()
      val job = backgroundScope.launch { a.repo.signOut() }
      advanceTimeBy(19_000)
      assertTrue(a.repo.state.value is SyncState.Ready)
      advanceTimeBy(2_000)
      job.join()
      assertEquals(SyncState.SignedOut, a.repo.state.value)
    }

  @Test
  fun signingOutFromRemovedHasNothingToRemove() =
    test {
      val a = Device(backgroundScope, "device-1", remote = HangingRemote(fake))
      a.activate()
      fake.removeFromOutside("device-1")
      a.runner.pushOutcome = SyncOutcome.Denied
      a.repo.push()
      assertTrue(a.repo.state.value is SyncState.Removed)
      a.repo.signOut()
      assertEquals(SyncState.SignedOut, a.repo.state.value)
    }

  @Test
  fun switchingToProWritesThePlan() =
    test {
      val a = device("device-1")
      a.activate()
      assertFalse(fake.pro)
      a.proFlow.value = true
      assertTrue(fake.pro)
    }

  @Test
  fun aProDeviceEnteringAFreeDocumentUsesTheProLimit() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1")
      val b = device("device-3", pro = true)
      b.enter(a.words())
      assertNull(b.repo.slotsPrompt.value)
      assertTrue(fake.pro)
      assertEquals(3, fake.deviceIds.size)
    }

  @Test
  fun downgradingFromProWithFiveDevicesKeepsThemAndBlocksNewOnes() =
    test {
      val a = device("device-1", pro = true)
      a.activate()
      repeat(PRO_DEVICES - 1) { a.seed("other-$it") }
      val words = a.words()
      a.proFlow.value = false
      assertFalse(fake.pro)
      assertEquals(PRO_DEVICES, fake.deviceIds.size)
      assertEquals(FREE_DEVICES, a.repo.deviceLimit.first())
      // El que ja hi és no veu mai les places plenes
      assertNull(a.repo.slotsPrompt.value)
      val b = device("device-9")
      b.enter(words)
      val prompt = b.repo.slotsPrompt.value
      assertNotNull(prompt)
      assertEquals(PRO_DEVICES, prompt!!.devices.size)
      assertEquals(PRO_DEVICES, fake.deviceIds.size)
    }

  @Test
  fun lastSyncAtIsTouchedAtMostEveryFifteenMinutes() =
    test {
      val a = device("device-1")
      a.activate()
      val first = fake.lastSyncAt("device-1")
      now += 10 * 60 * 1000L
      a.repo.push()
      assertEquals(first, fake.lastSyncAt("device-1"))
      now += 6 * 60 * 1000L
      a.repo.push()
      assertTrue(fake.lastSyncAt("device-1")!! > first!!)
    }

  @Test
  fun devicesAreListedWithTheirNamesAndCanBeRenamed() =
    test {
      val a = device("device-1")
      a.activate()
      a.seed("other-1", "Tablet")
      val list = a.repo.devices.first()
      assertEquals(listOf("Pixel", "Tablet"), list.map { it.name })
      assertEquals(listOf(true, false), list.map { it.isThis })
      assertEquals(DeviceActionOutcome.Done, a.repo.renameDevice("other-1", "Phone"))
      assertEquals(
        listOf("Pixel", "Phone"),
        a.repo.devices
          .first()
          .map { it.name },
      )
      assertEquals(DeviceActionOutcome.Done, a.repo.removeDevice("other-1"))
      assertEquals(listOf("device-1"), fake.deviceIds)
      assertEquals(FREE_DEVICES, a.repo.deviceLimit.first())
    }

  @Test
  fun anUnreadableNameIsNullAndCountedUnderSyncRecord() =
    test {
      val a = device("device-1")
      a.activate()
      fake.joinDevice(TestAccount.uid, RemoteDevice("other-1", ByteArray(4), false), removing = null)
      val list = a.repo.devices.first()
      assertNull(list.single { it.id == "other-1" }.name)
      assertEquals(listOf(ErrorSites.SYNC_RECORD), a.errors)
      assertFalse(list.toString().contains("Pixel"))
    }
}

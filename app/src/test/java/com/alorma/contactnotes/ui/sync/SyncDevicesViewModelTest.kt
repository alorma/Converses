package com.alorma.contactnotes.ui.sync

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alorma.contactnotes.data.sync.ApplyResult
import com.alorma.contactnotes.data.sync.ChannelInfo
import com.alorma.contactnotes.data.sync.LocalState
import com.alorma.contactnotes.data.sync.LocalSyncSource
import com.alorma.contactnotes.data.sync.PendingChanges
import com.alorma.contactnotes.data.sync.RecordType
import com.alorma.contactnotes.data.sync.RemoteDevice
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SyncData
import com.alorma.contactnotes.data.sync.SyncOutcome
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncRunner
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.record.SyncRecord
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeAuthGateway
import com.alorma.contactnotes.testing.FakeCursorStore
import com.alorma.contactnotes.testing.FakeDeviceStore
import com.alorma.contactnotes.testing.FakeSyncKeyStore
import com.alorma.contactnotes.testing.FakeSyncLocalStore
import com.alorma.contactnotes.testing.FakeSyncRemote
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import com.alorma.contactnotes.testing.TestAccount
import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class NoopRunner : SyncRunner {
  var pushOutcome = SyncOutcome.Done

  override suspend fun push(
    uid: String,
    cipher: RecordCipher,
  ) = pushOutcome

  override suspend fun pull(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit,
  ) = SyncOutcome.Done
}

private class NoopSource : LocalSyncSource {
  override suspend fun entryCount(): Int = 0

  override fun observePendingCount(): Flow<Int> = MutableStateFlow(0)

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

/** Les accions de dispositius i places, amb un compte i un Firestore de mentida compartits per dos dispositius. */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncDevicesViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val scope = TestScope(UnconfinedTestDispatcher())
  private val accounts = FakeAccountRemote()
  private val fake = FakeSyncRemote()
  private val tracker = RecordingTracker()
  private val store = ViewModelStore()

  private inner class Device(
    id: String,
  ) {
    val keyStore = FakeSyncKeyStore()
    val runner = NoopRunner()
    val repo =
      SyncRepository(
        FakeAuthGateway(),
        accounts,
        keyStore,
        FakeSyncLocalStore(),
        scope.backgroundScope,
        clock = { 1_000_000L },
        sync =
          SyncData(
            runner,
            NoopSource(),
            fake,
            FakeCursorStore(deviceId = id),
            FakeDeviceStore(id, false),
            deviceName = { "Pixel" },
            isPro = { false },
            isProChanges = MutableStateFlow(false),
          ),
      )

    suspend fun seed(
      otherId: String,
      name: String,
    ) {
      val cipher = RecordCipher(keyStore.stored!!.dataKey, TestAccount.uid)
      val sealed = cipher.encrypt("device-name:$otherId", name.toByteArray())
      fake.joinDevice(TestAccount.uid, RemoteDevice(otherId, sealed, fake.pro), removing = null)
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private inline fun <reified T : ViewModel> viewModel(
    type: KClass<T>,
    noinline create: () -> T,
  ): T = ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[T::class]

  private fun syncViewModel(repo: SyncRepository) =
    viewModel(SyncViewModel::class) { SyncViewModel(repo, tracker.eventTracker, scope) }

  private fun promptsViewModel(repo: SyncRepository) =
    viewModel(SyncPromptsViewModel::class) { SyncPromptsViewModel(repo, tracker.eventTracker) }

  private suspend fun activated(): Device {
    val a = Device("device-1")
    a.repo.signIn(Activity())
    a.repo.confirmActivation()
    return a
  }

  /** Dos dispositius amb el compte ple (A i «other-1») i un tercer, B, que acaba d'entrar i veu les places plenes. */
  private suspend fun fullAccount(): Pair<Device, Device> {
    val a = activated()
    a.seed("other-1", "Tablet")
    a.repo.unlockWords()
    val b = Device("device-3")
    b.repo.signIn(Activity())
    b.repo.submitWords(a.repo.securityWords()!!)
    return a to b
  }

  @Test
  fun renamingADeviceIsTrackedWithoutTheName() =
    scope.runTest {
      val a = activated()
      a.seed("other-1", "Tablet")
      syncViewModel(a.repo).onRename("other-1", "My tablet")
      assertEquals(listOf("sync_device_renamed" to emptyMap<String, String>()), tracker.recordedActions())
    }

  @Test
  fun removingFromTheListIsTrackedAsList() =
    scope.runTest {
      val a = activated()
      a.seed("other-1", "Tablet")
      syncViewModel(a.repo).onRemove("other-1")
      assertEquals(listOf("sync_device_removed" to mapOf("from" to "list")), tracker.recordedActions())
    }

  @Test
  fun anActionThatFailsIsNotTracked() =
    scope.runTest {
      val a = activated()
      a.seed("other-1", "Tablet")
      fake.failure = RemoteResult.Offline
      syncViewModel(a.repo).onRemove("other-1")
      assertTrue(tracker.recordedActions().isEmpty())
    }

  @Test
  fun rejoiningWhenNotRemovedIsTrackedAsFailed() =
    scope.runTest {
      val a = activated()
      syncViewModel(a.repo).onRejoin()
      assertEquals(listOf("sync_rejoined" to mapOf("result" to "failed")), tracker.recordedActions())
    }

  @Test
  fun theSlotsPromptOpensTheRouteAndIsCountedOnce() =
    scope.runTest {
      val (_, b) = fullAccount()
      val prompts = promptsViewModel(b.repo)
      assertTrue(prompts.slotsOpen.value)
      assertEquals(listOf("sync_slots_shown" to emptyMap<String, String>()), tracker.recordedActions())
    }

  @Test
  fun choosingADeviceInSlotsIsTrackedAsSlotsAndClosesThePrompt() =
    scope.runTest {
      val (_, b) = fullAccount()
      val prompts = promptsViewModel(b.repo)
      val slots = viewModel(SlotsViewModel::class) { SlotsViewModel(b.repo, tracker.eventTracker) }
      slots.onRemoveAndContinue("other-1")
      assertFalse(prompts.slotsOpen.value)
      assertEquals(
        listOf("sync_slots_shown" to emptyMap(), "sync_device_removed" to mapOf("from" to "slots")),
        tracker.recordedActions(),
      )
    }

  @Test
  fun notNowSignsOut() =
    scope.runTest {
      val (_, b) = fullAccount()
      viewModel(SlotsViewModel::class) { SlotsViewModel(b.repo, tracker.eventTracker) }.onNotNow()
      assertEquals(SyncState.SignedOut, b.repo.state.value)
    }

  @Test
  fun removedSeenIsCountedOncePerArrivalAndResetsOnRejoin() =
    scope.runTest {
      val a = activated()
      promptsViewModel(a.repo)

      fun seen() = tracker.recordedActions().count { it.first == "sync_removed_seen" }

      fake.removeFromOutside("device-1")
      a.runner.pushOutcome = SyncOutcome.Denied
      a.repo.push()
      assertTrue(a.repo.state.value is SyncState.Removed)
      a.repo.push()
      assertEquals(1, seen())

      assertEquals(
        "done",
        a.repo
          .rejoin()
          .name
          .lowercase(),
      )
      assertTrue(a.repo.state.value is SyncState.Ready)
      a.runner.pushOutcome = SyncOutcome.Done

      fake.removeFromOutside("device-1")
      a.runner.pushOutcome = SyncOutcome.Denied
      a.repo.push()
      assertTrue(a.repo.state.value is SyncState.Removed)
      assertEquals(2, seen())
    }
}

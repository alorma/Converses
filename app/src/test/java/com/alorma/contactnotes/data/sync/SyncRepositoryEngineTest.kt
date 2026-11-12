package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import com.alorma.contactnotes.data.sync.record.SyncRecord
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeAuthGateway
import com.alorma.contactnotes.testing.FakeCursorStore
import com.alorma.contactnotes.testing.FakeSyncKeyStore
import com.alorma.contactnotes.testing.FakeSyncLocalStore
import com.alorma.contactnotes.testing.FakeSyncRemote
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRunner : SyncRunner {
  val calls = mutableListOf<String>()
  var pushOutcome = SyncOutcome.Done
  var pullOutcome = SyncOutcome.Done
  var onPush: suspend () -> Unit = {}
  var onPull: (onProgress: (Int, Int?) -> Unit) -> Unit = {}

  override suspend fun push(
    uid: String,
    cipher: RecordCipher,
  ): SyncOutcome {
    calls += "push"
    onPush()
    return pushOutcome
  }

  override suspend fun pull(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit,
  ): SyncOutcome {
    calls += "pull"
    onPull(onProgress)
    return pullOutcome
  }
}

private class FakeSource(
  val pending: MutableStateFlow<Int> = MutableStateFlow(0),
  var entries: Int = 0,
) : LocalSyncSource {
  override suspend fun entryCount(): Int = entries

  override fun observePendingCount(): Flow<Int> = pending

  override suspend fun readPending(): PendingChanges = error("not used")

  override suspend fun markClean(
    syncId: String,
    type: RecordType,
    modifiedAt: Long,
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

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryEngineTest {
  private val scope = TestScope(UnconfinedTestDispatcher())
  private val runner = FakeRunner()
  private val source = FakeSource()
  private val records = FakeSyncRemote()
  private val accounts = FakeAccountRemote()
  private val keyStore = FakeSyncKeyStore()
  private val auth = FakeAuthGateway()
  private val repository =
    SyncRepository(
      auth,
      accounts,
      keyStore,
      FakeSyncLocalStore(),
      scope,
      clock = { 7_000L },
      sync = SyncData(runner, source, records, FakeCursorStore(), deviceName = { "Pixel" }, isPro = { true }),
    )
  private val activity = Activity()

  private suspend fun activate() {
    repository.signIn(activity)
    repository.confirmActivation()
  }

  /** Un compte que ja té dades des d'un altre dispositiu; retorna les seves paraules. */
  private suspend fun joinExistingAccount(): List<String> {
    val secret = RecoverySecret.generate()
    accounts.accounts[TestAccount.uid] =
      RemoteAccount(KeyWrap.wrap(DataKey.generate(), secret, TestAccount.uid), SYNC_FORMAT)
    repository.signIn(activity)
    return secret.words
  }

  @Test
  fun statusGoesFromSyncingToUpToDate() =
    scope.runTest {
      activate()
      val gate = CompletableDeferred<Unit>()
      runner.onPush = { gate.await() }
      val job = scope.launch { repository.push() }
      assertTrue(repository.status.value is SyncStatus.Syncing)
      gate.complete(Unit)
      job.join()
      assertEquals(SyncStatus.UpToDate(7_000L), repository.status.value)
    }

  @Test
  fun offlineKeepsThePendingCount() =
    scope.runTest {
      activate()
      source.pending.value = 3
      runner.pushOutcome = SyncOutcome.Offline
      assertEquals(SyncOutcome.Offline, repository.push())
      assertEquals(SyncStatus.Offline(3), repository.status.value)
    }

  @Test
  fun aFailureIsReported() =
    scope.runTest {
      activate()
      runner.pullOutcome = SyncOutcome.Failed
      repository.pull()
      assertEquals(SyncStatus.Failed, repository.status.value)
    }

  @Test
  fun syncNowPushesAndThenPullsButStopsAtAFailedPush() =
    scope.runTest {
      activate()
      runner.calls.clear()
      repository.syncNow()
      assertEquals(listOf("push", "pull"), runner.calls)
      runner.calls.clear()
      runner.pushOutcome = SyncOutcome.Offline
      repository.syncNow()
      assertEquals(listOf("push"), runner.calls)
    }

  @Test
  fun theDeviceIsRegisteredWithASealedNameBeforeTheFirstPush() =
    scope.runTest {
      source.pending.value = 1
      var registeredAtPush = false
      runner.onPush = { registeredAtPush = records.devices.isNotEmpty() }
      activate()
      assertTrue(registeredAtPush)
      val device = records.devices.single()
      assertEquals("device-1", device.id)
      assertTrue(device.pro)
      val cipher = RecordCipher(keyStore.stored!!.dataKey, TestAccount.uid)
      assertEquals("Pixel", cipher.decrypt("device-name:device-1", device.sealedName)?.decodeToString())
    }

  @Test
  fun activatingUploadsEverythingWithProgress() =
    scope.runTest {
      source.pending.value = 4
      val seen = mutableListOf<InitialSync?>()
      runner.onPush = {
        source.pending.value = 2
        kotlinx.coroutines.yield()
        seen += repository.initialSync.value
      }
      activate()
      assertEquals(listOf<InitialSync?>(InitialSync.Uploading(0.5f)), seen)
      assertEquals(InitialSync.Done, repository.initialSync.value)
      assertEquals(listOf("push"), runner.calls)
    }

  @Test
  fun anOfflineInitialUploadCanBeRetried() =
    scope.runTest {
      runner.pushOutcome = SyncOutcome.Offline
      activate()
      assertEquals(InitialSync.Offline, repository.initialSync.value)
      runner.pushOutcome = SyncOutcome.Done
      repository.retryInitialSync()
      assertEquals(InitialSync.Done, repository.initialSync.value)
      repository.dismissInitialSync()
      assertNull(repository.initialSync.value)
    }

  @Test
  fun aNewDeviceWithoutEntriesJustDownloads() =
    scope.runTest {
      val seen = mutableListOf<InitialSync?>()
      runner.onPull = { progress ->
        progress(1, 4)
        seen += repository.initialSync.value
      }
      val words = joinExistingAccount()
      assertEquals(WordsOutcome.Accepted, repository.submitWords(words))
      assertEquals(listOf<InitialSync?>(InitialSync.Downloading(0.25f)), seen)
      assertEquals(InitialSync.Done, repository.initialSync.value)
      assertEquals(listOf("pull"), runner.calls)
    }

  @Test
  fun aNewDeviceWithEntriesAsksBeforeMergingAndThenDownloads() =
    scope.runTest {
      source.entries = 5
      val words = joinExistingAccount()
      repository.submitWords(words)
      assertEquals(InitialSync.MergePrompt(5), repository.initialSync.value)
      assertTrue(runner.calls.isEmpty())
      repository.continueMerge()
      assertEquals(listOf("push", "pull"), runner.calls)
      assertEquals(InitialSync.Done, repository.initialSync.value)
    }

  @Test
  fun signingOutStopsEverything() =
    scope.runTest {
      activate()
      runner.calls.clear()
      repository.signOut()
      assertEquals(SyncOutcome.Done, repository.push())
      assertEquals(SyncOutcome.Done, repository.pull())
      assertTrue(runner.calls.isEmpty())
      assertNull(repository.initialSync.value)
      assertEquals(SyncStatus.UpToDate(null), repository.status.value)
    }

  @Test
  fun withoutASessionNothingRuns() =
    scope.runTest {
      repository.push()
      repository.pull()
      assertTrue(runner.calls.isEmpty())
      assertTrue(records.devices.isEmpty())
    }
}

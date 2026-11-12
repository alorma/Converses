package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeAuthGateway
import com.alorma.contactnotes.testing.FakeSyncKeyStore
import com.alorma.contactnotes.testing.FakeSyncLocalStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeActions : SyncActions {
  val pending = MutableStateFlow(0)
  val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
  val pushTimes = mutableListOf<Long>()
  val pullTimes = mutableListOf<Long>()
  var pushResults = ArrayDeque<SyncOutcome>()
  var pullResults = ArrayDeque<SyncOutcome>()
  var clock: () -> Long = { 0L }

  override suspend fun push(): SyncOutcome {
    pushTimes += clock()
    val outcome = pushResults.removeFirstOrNull() ?: SyncOutcome.Done
    if (outcome == SyncOutcome.Done) pending.value = 0
    return outcome
  }

  override suspend fun pull(): SyncOutcome {
    pullTimes += clock()
    return pullResults.removeFirstOrNull() ?: SyncOutcome.Done
  }

  override val pendingCount: Flow<Int> get() = pending

  override fun remoteChanges(): Flow<Unit> = flow { changes.collect { emit(it) } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSchedulerTest {
  @Test
  fun severalChangesMakeOnePushTwoSecondsAfterTheLast() =
    runTest {
      val actions = FakeActions().also { it.clock = { currentTime } }
      backgroundScope.launch { SyncLoop(actions).run() }
      runCurrent()
      actions.pending.value = 1
      advanceTimeBy(1_000)
      actions.pending.value = 2
      advanceTimeBy(1_500)
      actions.pending.value = 3
      advanceTimeBy(1_999)
      assertTrue(actions.pushTimes.isEmpty())
      advanceTimeBy(2)
      assertEquals(listOf(4_500L), actions.pushTimes)
    }

  @Test
  fun aFailedPushRetriesWithGrowingWaits() =
    runTest {
      val actions = FakeActions().also { it.clock = { currentTime } }
      actions.pushResults = ArrayDeque(List(5) { SyncOutcome.Offline })
      backgroundScope.launch { SyncLoop(actions).run() }
      runCurrent()
      actions.pending.value = 1
      advanceTimeBy(2_000 + 5_000 + 15_000 + 60_000 + 300_000 + 300_000 + 1)
      // 2 s de debounce, i després 5 s, 15 s, 1 min, 5 min i 5 min (l'última es repeteix)
      assertEquals(listOf(2_000L, 7_000L, 22_000L, 82_000L, 382_000L, 682_000L), actions.pushTimes)
    }

  @Test
  fun backoffSequenceIsFixedAndTheLastRepeats() {
    val loop = SyncLoop(FakeActions())
    assertEquals(listOf(5_000L, 15_000L, 60_000L, 300_000L, 300_000L), (0..4).map { loop.backoff(it) })
  }

  @Test
  fun itPullsOnStartAndWhenAnotherDeviceWrites() =
    runTest {
      val actions = FakeActions().also { it.clock = { currentTime } }
      backgroundScope.launch { SyncLoop(actions).run() }
      runCurrent()
      assertEquals(listOf(0L), actions.pullTimes)
      advanceTimeBy(3_000)
      actions.changes.tryEmit(Unit)
      runCurrent()
      assertEquals(listOf(0L, 3_000L), actions.pullTimes)
    }

  @Test
  fun aFailedPullIsRetried() =
    runTest {
      val actions = FakeActions().also { it.clock = { currentTime } }
      actions.pullResults = ArrayDeque(listOf(SyncOutcome.Failed))
      backgroundScope.launch { SyncLoop(actions).run() }
      advanceTimeBy(5_001)
      assertEquals(listOf(0L, 5_000L), actions.pullTimes)
    }

  @Test
  fun nothingRunsWithoutASession() =
    runTest {
      val repository =
        SyncRepository(
          FakeAuthGateway(),
          FakeAccountRemote(),
          FakeSyncKeyStore(),
          FakeSyncLocalStore(),
          backgroundScope,
        )
      var enqueued = 0
      val visible = MutableStateFlow(true)
      SyncScheduler(repository, { enqueued++ }, backgroundScope).start(visible)
      runCurrent()
      visible.value = false
      runCurrent()
      assertEquals(0, enqueued)
      assertEquals(SyncStatus.UpToDate(null), repository.status.value)
      assertEquals(SyncState.SignedOut, repository.state.value)
    }
}

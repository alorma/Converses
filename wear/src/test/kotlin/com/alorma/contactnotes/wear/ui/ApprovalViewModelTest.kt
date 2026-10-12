package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearApprovalDecision
import com.alorma.contactnotes.shared.WearApprovalResult
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.wear.data.ApprovalAckResult
import com.alorma.contactnotes.wear.data.ApprovalClient
import com.alorma.contactnotes.wear.data.ProtectedSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApprovalViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  private class FakeClient(
    var ack: ApprovalAckResult = ApprovalAckResult.Pending,
  ) : ApprovalClient {
    val requested = mutableListOf<Pair<String, Long>>()
    val cancelled = mutableListOf<String>()

    override suspend fun request(
      requestId: String,
      personId: Long,
    ): ApprovalAckResult {
      requested += requestId to personId
      return ack
    }

    override fun cancel(requestId: String) {
      cancelled += requestId
    }
  }

  private val client = FakeClient()
  private val results = MutableSharedFlow<WearApprovalResult>(extraBufferCapacity = 8)
  private val sessions = ProtectedSessions()
  private var nextId = 0

  @Before
  fun setUp() = Dispatchers.setMain(dispatcher)

  @After
  fun tearDown() = Dispatchers.resetMain()

  private fun viewModel() =
    ApprovalViewModel(
      personId = 7,
      client = client,
      results = results,
      sessions = sessions,
      newId = { "req-${++nextId}" },
      ttlMillis = 60_000,
    )

  private fun entry(id: Long) = WearEntry(id, EntryKind.SAID, ChannelType.CHAT, "e$id", id)

  private fun TestScope.startedViewModel(): ApprovalViewModel = viewModel().also { runCurrent() }

  @Test
  fun startsWaitingAndSendsOneRequestForThatPerson() =
    runTest {
      val vm = startedViewModel()

      assertEquals(ApprovalUiState.Waiting(60, 60), vm.state.value)
      assertEquals(listOf("req-1" to 7L), client.requested)
    }

  @Test
  fun theCountdownTicksOncePerSecond() =
    runTest {
      val vm = startedViewModel()

      advanceTimeBy(18_500)

      assertEquals(ApprovalUiState.Waiting(42, 60), vm.state.value)
    }

  @Test
  fun approvalOpensASessionWithTheEntriesAndTheToken() =
    runTest {
      val vm = startedViewModel()

      results.emit(
        WearApprovalResult(
          requestId = "req-1",
          decision = WearApprovalDecision.APPROVED,
          entries = listOf(entry(2), entry(1)),
          hasMore = true,
          noteToken = "tok",
          noteTokenExpiresAt = 99,
        ),
      )
      runCurrent()

      assertEquals(ApprovalUiState.Approved, vm.state.value)
      val session = sessions.get(7)!!
      assertEquals(listOf(2L, 1L), session.entries.map { it.id })
      assertEquals("tok", session.noteToken)
    }

  @Test
  fun denialShowsDeniedAndOpensNothing() =
    runTest {
      val vm = startedViewModel()

      results.emit(WearApprovalResult(requestId = "req-1", decision = WearApprovalDecision.DENIED))
      runCurrent()

      assertEquals(ApprovalUiState.Denied, vm.state.value)
      assertNull(sessions.get(7))
    }

  @Test
  fun aResultForAnotherRequestIsIgnored() =
    runTest {
      val vm = startedViewModel()

      results.emit(WearApprovalResult(requestId = "altra", decision = WearApprovalDecision.DENIED))
      runCurrent()

      assertTrue(vm.state.value is ApprovalUiState.Waiting)
    }

  @Test
  fun withoutAnAnswerItExpiresAndTheNotificationIsWithdrawn() =
    runTest {
      val vm = startedViewModel()

      advanceTimeBy(60_001)
      runCurrent()

      assertEquals(ApprovalUiState.Expired, vm.state.value)
      assertEquals(listOf("req-1"), client.cancelled)
      assertNull(sessions.get(7))
    }

  @Test
  fun retryStartsAFreshRequestWithANewId() =
    runTest {
      val vm = startedViewModel()
      advanceTimeBy(60_001)
      runCurrent()

      vm.retry()
      runCurrent()

      assertEquals(listOf("req-1", "req-2"), client.requested.map { it.first })
      assertEquals(ApprovalUiState.Waiting(60, 60), vm.state.value)
    }

  @Test
  fun noPhoneShowsTheUnavailableScreen() =
    runTest {
      client.ack = ApprovalAckResult.Unavailable
      val vm = startedViewModel()

      assertEquals(ApprovalUiState.NoPhone, vm.state.value)
    }

  @Test
  fun aRejectedRequestShowsDenied() =
    runTest {
      client.ack = ApprovalAckResult.Rejected
      val vm = startedViewModel()

      assertEquals(ApprovalUiState.Denied, vm.state.value)
    }

  @Test
  fun cancelTellsThePhoneAndStopsListening() =
    runTest {
      val vm = startedViewModel()

      vm.cancel()
      results.emit(
        WearApprovalResult(requestId = "req-1", decision = WearApprovalDecision.APPROVED, entries = listOf(entry(1))),
      )
      runCurrent()

      assertEquals(listOf("req-1"), client.cancelled)
      assertNull(sessions.get(7))
    }
}

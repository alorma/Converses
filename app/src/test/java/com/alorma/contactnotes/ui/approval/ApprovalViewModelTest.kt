package com.alorma.contactnotes.ui.approval

import com.alorma.contactnotes.data.wear.ApprovalNotifier
import com.alorma.contactnotes.data.wear.ApprovalTokens
import com.alorma.contactnotes.data.wear.WearApprovalCoordinator
import com.alorma.contactnotes.shared.WEAR_APPROVAL_TTL_MS
import com.alorma.contactnotes.shared.WearApprovalCancel
import com.alorma.contactnotes.shared.WearApprovalCodec
import com.alorma.contactnotes.shared.WearApprovalDecision
import com.alorma.contactnotes.shared.WearApprovalRequest
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ApprovalViewModelTest {
  @get:Rule val main = MainDispatcherRule()

  private val db = FakeDb()
  private var now = 1_000_000L
  private val sent = mutableListOf<String>()

  private val coordinator =
    WearApprovalCoordinator(
      repository = db.repository,
      notifier =
        object : ApprovalNotifier {
          override fun show(requestId: String) = Unit

          override fun cancel(requestId: String) = Unit
        },
      sender = { _, payload -> sent += payload },
      tokens = ApprovalTokens(clock = { now }),
      clock = { now },
    )

  private suspend fun pendingRequest(): Long {
    val person = db.addPerson("Secret", isProtected = true)
    db.addEntry(person.id, text = "confidencial")
    coordinator.onRequest(
      "watch",
      WearApprovalCodec.encodeRequest(WearApprovalRequest(requestId = "req", personId = person.id)),
    )
    return person.id
  }

  private fun lastDecision() = WearApprovalCodec.decodeResult(sent.last())!!.decision

  @Test
  fun aPendingRequestStartsByOpeningThePromptRightAway() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)
      assertEquals(ApprovalUiState.Prompting, vm.state.value)
      assertTrue(vm.takePromptRequest())
      assertFalse(vm.takePromptRequest())
    }

  @Test
  fun anUnknownCancelledOrExpiredRequestFailsRightAway() =
    runTest {
      assertEquals(ApprovalUiState.Failed, ApprovalViewModel("nope", coordinator).state.value)

      pendingRequest()
      now += WEAR_APPROVAL_TTL_MS + 1
      assertEquals(ApprovalUiState.Failed, ApprovalViewModel("req", coordinator).state.value)
    }

  @Test
  fun aSuccessfulBiometricApprovesAndSendsTheEntries() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)

      vm.onAuthenticated()

      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertEquals(WearApprovalDecision.APPROVED, lastDecision())
      assertTrue(
        WearApprovalCodec
          .decodeResult(sent.last())!!
          .entries
          .single()
          .text == "confidencial",
      )
    }

  @Test
  fun denyOrAFailedBiometricClosesAndTellsTheWatchWithNoContent() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)

      vm.onDeny()

      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertEquals(WearApprovalDecision.DENIED, lastDecision())
      assertTrue(WearApprovalCodec.decodeResult(sent.last())!!.entries.isEmpty())
    }

  @Test
  fun aLateBiometricAfterTheWatchCancelledIsIgnored() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)
      coordinator.onCancel(WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = "req")))

      vm.onAuthenticated()

      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertTrue(sent.isEmpty())
    }

  @Test
  fun whenTheWatchCancelsTheOpenScreenClosesByItself() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)

      coordinator.onCancel(WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = "req")))

      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertTrue(sent.isEmpty())
    }

  @Test
  fun whenTheWatchCancelsWhileTheScreenWithButtonsIsShownItAlsoCloses() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)
      vm.takePromptRequest()
      vm.onPromptCancelled()

      coordinator.onCancel(WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = "req")))

      assertEquals(ApprovalUiState.Closed, vm.state.value)
    }

  @Test
  fun aCancellationOfAnotherRequestIsIgnored() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)

      coordinator.onCancel(WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = "altra")))

      assertEquals(ApprovalUiState.Prompting, vm.state.value)
    }

  @Test
  fun whenTheRequestExpiresWhileOpenTheScreenCloses() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)

      now += WEAR_APPROVAL_TTL_MS + 2_000
      advanceTimeBy(WEAR_APPROVAL_TTL_MS + 1_500)

      assertEquals(ApprovalUiState.Closed, vm.state.value)
    }

  @Test
  fun cancellingThePromptShowsTheScreenInsteadOfDenying() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)
      vm.takePromptRequest()

      vm.onPromptCancelled()

      assertEquals(ApprovalUiState.Confirm, vm.state.value)
      assertTrue(sent.isEmpty())
      assertTrue(coordinator.isPending("req"))
    }

  @Test
  fun confirmOnTheScreenReopensThePromptAndCanStillApprove() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)
      vm.takePromptRequest()
      vm.onPromptCancelled()

      vm.onRetry()

      assertEquals(ApprovalUiState.Prompting, vm.state.value)
      assertTrue(vm.takePromptRequest())
      vm.onAuthenticated()
      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertEquals(WearApprovalDecision.APPROVED, lastDecision())
    }

  @Test
  fun denyOnTheScreenTellsTheWatch() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)
      vm.takePromptRequest()
      vm.onPromptCancelled()

      vm.onDeny()

      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertEquals(WearApprovalDecision.DENIED, lastDecision())
    }

  @Test
  fun aSecondAnswerIsIgnored() =
    runTest {
      pendingRequest()
      val vm = ApprovalViewModel("req", coordinator)

      vm.onAuthenticated()
      vm.onDeny()

      assertEquals(ApprovalUiState.Closed, vm.state.value)
      assertEquals(1, sent.size)
    }
}

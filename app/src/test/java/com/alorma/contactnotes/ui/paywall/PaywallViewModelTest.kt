package com.alorma.contactnotes.ui.paywall

import android.app.Activity
import com.alorma.contactnotes.data.pro.NetworkMonitor
import com.alorma.contactnotes.data.pro.ProDataSource
import com.alorma.contactnotes.data.pro.ProRepository
import com.alorma.contactnotes.data.pro.PurchaseResult
import com.alorma.contactnotes.data.pro.RestoreResult
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PaywallViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private class FakeProDataSource : ProDataSource {
    val pro = MutableStateFlow(false)
    var priceValue: String? = "4,99 €"
    var priceCalls = 0
    var purchaseResult: PurchaseResult = PurchaseResult.Purchased
    var restoreResult: RestoreResult = RestoreResult.Restored
    var purchaseGate: CompletableDeferred<Unit>? = null

    override val isPro: StateFlow<Boolean> = pro

    override suspend fun price(): String? {
      priceCalls++
      return priceValue
    }

    override suspend fun purchase(activity: Activity): PurchaseResult {
      purchaseGate?.await()
      return purchaseResult
    }

    override suspend fun restore(): RestoreResult = restoreResult
  }

  private class FakeNetwork(
    online: Boolean = true,
  ) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)
  }

  private val source = FakeProDataSource()
  private val network = FakeNetwork()

  private val tracker = RecordingTracker()

  private fun viewModel() = PaywallViewModel(ProRepository(source), network, tracker.eventTracker)

  private suspend fun PaywallViewModel.current(): PaywallState = state.first()

  @Test
  fun benefitsKeepTheDesignOrderFromSettingsAndPutTheOriginFirst() {
    assertEquals(
      listOf(ProBenefit.People, ProBenefit.Channels, ProBenefit.FollowUps),
      proBenefits(null),
    )
    assertEquals(
      listOf(ProBenefit.FollowUps, ProBenefit.People, ProBenefit.Channels),
      proBenefits(ProBenefit.FollowUps),
    )
  }

  @Test
  fun loadsThePriceAndCanBuy() =
    runTest {
      val state = viewModel().current()

      assertEquals("4,99 €", state.price)
      assertTrue(state.canBuy)
      assertFalse(state.isPro)
    }

  @Test
  fun withoutAPriceTheButtonStaysDisabled() =
    runTest {
      source.priceValue = null

      val state = viewModel().current()

      assertNull(state.price)
      assertFalse(state.canBuy)
    }

  @Test
  fun offlineDisablesBuyAndRestoreAndLoadsThePriceWhenBack() =
    runTest {
      network.isOnline.value = false
      val viewModel = viewModel()

      val offline = viewModel.current()
      assertFalse(offline.isOnline)
      assertFalse(offline.canBuy)
      assertFalse(offline.canRestore)
      assertEquals(0, source.priceCalls)

      network.isOnline.value = true

      val online = viewModel.current()
      assertEquals("4,99 €", online.price)
      assertTrue(online.canBuy)
    }

  @Test
  fun alreadyProShowsYouHavePro() =
    runTest {
      source.pro.value = true

      assertTrue(viewModel().current().isPro)
    }

  @Test
  fun purchaseShowsYouHaveProWithoutWaitingForTheListener() =
    runTest {
      val viewModel = viewModel()

      viewModel.onBuy(Activity())
      viewModel.onRestore()

      assertTrue(viewModel.current().isPro)
      assertEquals(
        listOf("purchase_started" to emptyMap<String, String>(), "restore_tapped" to emptyMap()),
        tracker.recordedActions(),
      )
    }

  @Test
  fun busyWhilePurchasing() =
    runTest {
      val gate = CompletableDeferred<Unit>()
      source.purchaseGate = gate
      val viewModel = viewModel()

      viewModel.onBuy(Activity())
      val busy = viewModel.current()
      assertTrue(busy.busy)
      assertFalse(busy.canBuy)
      assertFalse(busy.canRestore)

      gate.complete(Unit)
      assertFalse(viewModel.current().busy)
    }

  @Test
  fun purchaseOutcomesAndTheirMessages() =
    runTest {
      val expected =
        mapOf(
          PurchaseResult.Pending to listOf(PaywallMessage.Pending),
          PurchaseResult.Failed to listOf(PaywallMessage.Failed),
          PurchaseResult.Offline to listOf(PaywallMessage.Failed),
          PurchaseResult.Cancelled to emptyList(),
        )
      expected.forEach { (result, messages) ->
        source.purchaseResult = result
        val viewModel = viewModel()
        val received = mutableListOf<PaywallMessage>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(received) }

        viewModel.onBuy(Activity())

        assertEquals("$result", messages, received)
        assertFalse("$result", viewModel.current().isPro)
        job.cancel()
      }
    }

  @Test
  fun restoreOutcomesAndTheirMessages() =
    runTest {
      val expected =
        mapOf(
          RestoreResult.NothingToRestore to listOf(PaywallMessage.NothingToRestore),
          RestoreResult.Pending to listOf(PaywallMessage.Pending),
          RestoreResult.Failed to listOf(PaywallMessage.Failed),
          RestoreResult.Offline to listOf(PaywallMessage.Failed),
        )
      expected.forEach { (result, messages) ->
        source.restoreResult = result
        val viewModel = viewModel()
        val received = mutableListOf<PaywallMessage>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(received) }

        viewModel.onRestore()

        assertEquals("$result", messages, received)
        assertFalse("$result", viewModel.current().isPro)
        job.cancel()
      }
    }

  private fun TestScope.messagesOf(viewModel: PaywallViewModel): Pair<MutableList<PaywallMessage>, Job> {
    val received = mutableListOf<PaywallMessage>()
    return received to launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(received) }
  }

  /** Backlog 18: RevenueCat no pot enviar la compra i `awaitPurchase` no torna mai. */
  @Test
  fun aPurchaseThatNeverReturnsIsPendingAfterTheTimeoutOnceBack() =
    runTest {
      source.purchaseGate = CompletableDeferred()
      val viewModel = viewModel()
      val (received, job) = messagesOf(viewModel)

      viewModel.onBuy(Activity())
      viewModel.onResumed()
      advanceTimeBy(PURCHASE_TIMEOUT - 1.milliseconds)
      assertTrue(viewModel.current().busy)
      assertEquals(emptyList<PaywallMessage>(), received)

      advanceTimeBy(2.milliseconds)
      val state = viewModel.current()
      assertFalse(state.busy)
      assertTrue(state.canBuy)
      assertTrue(state.canRestore)
      assertFalse(state.isPro)
      assertEquals(listOf(PaywallMessage.Pending), received)
      job.cancel()
    }

  @Test
  fun noPendingMessageIfProArrivedFromTheListenerMeanwhile() =
    runTest {
      source.purchaseGate = CompletableDeferred()
      val viewModel = viewModel()
      val (received, job) = messagesOf(viewModel)

      viewModel.onBuy(Activity())
      viewModel.onResumed()
      source.pro.value = true
      advanceTimeBy(PURCHASE_TIMEOUT + 1.seconds)

      assertTrue(viewModel.current().isPro)
      assertFalse(viewModel.current().busy)
      assertEquals(emptyList<PaywallMessage>(), received)
      job.cancel()
    }

  @Test
  fun noTimeoutWhileThePlaySheetIsOnTop() =
    runTest {
      source.purchaseGate = CompletableDeferred()
      val viewModel = viewModel()

      viewModel.onBuy(Activity())
      advanceTimeBy(10.minutes)

      assertTrue(viewModel.current().busy)
    }

  @Test
  fun aPurchaseBeforeTheTimeoutShowsYouHaveProAndNothingElse() =
    runTest {
      val gate = CompletableDeferred<Unit>()
      source.purchaseGate = gate
      val viewModel = viewModel()
      val (received, job) = messagesOf(viewModel)

      viewModel.onBuy(Activity())
      viewModel.onResumed()
      advanceTimeBy(5.seconds)
      gate.complete(Unit)
      advanceTimeBy(PURCHASE_TIMEOUT)

      assertTrue(viewModel.current().isPro)
      assertFalse(viewModel.current().busy)
      assertEquals(emptyList<PaywallMessage>(), received)
      job.cancel()
    }

  @Test
  fun aLatePurchaseStillShowsYouHavePro() =
    runTest {
      val gate = CompletableDeferred<Unit>()
      source.purchaseGate = gate
      val viewModel = viewModel()
      val (received, job) = messagesOf(viewModel)

      viewModel.onBuy(Activity())
      viewModel.onResumed()
      advanceTimeBy(PURCHASE_TIMEOUT + 1.seconds)
      gate.complete(Unit)

      assertTrue(viewModel.current().isPro)
      assertEquals(listOf(PaywallMessage.Pending), received)
      job.cancel()
    }

  @Test
  fun aLateFailureSaysSo() =
    runTest {
      val gate = CompletableDeferred<Unit>()
      source.purchaseGate = gate
      source.purchaseResult = PurchaseResult.Failed
      val viewModel = viewModel()
      val (received, job) = messagesOf(viewModel)

      viewModel.onBuy(Activity())
      viewModel.onResumed()
      advanceTimeBy(PURCHASE_TIMEOUT + 1.seconds)
      gate.complete(Unit)

      assertFalse(viewModel.current().isPro)
      assertFalse(viewModel.current().busy)
      assertEquals(listOf(PaywallMessage.Pending, PaywallMessage.Failed), received)
      job.cancel()
    }

  @Test
  fun buyingAgainWhileTheFirstIsStuckIsPending() =
    runTest {
      source.purchaseGate = CompletableDeferred()
      val viewModel = viewModel()
      val (received, job) = messagesOf(viewModel)
      viewModel.onBuy(Activity())
      viewModel.onResumed()
      advanceTimeBy(PURCHASE_TIMEOUT + 1.seconds)

      // RevenueCat respon `OperationAlreadyInProgressError`
      source.purchaseGate = null
      source.purchaseResult = PurchaseResult.Pending
      viewModel.onBuy(Activity())

      assertFalse(viewModel.current().busy)
      assertEquals(listOf(PaywallMessage.Pending, PaywallMessage.Pending), received)
      job.cancel()
    }

  @Test
  fun restoreWithAPurchaseShowsYouHavePro() =
    runTest {
      val viewModel = viewModel()

      viewModel.onRestore()

      assertTrue(viewModel.current().isPro)
    }

  @Test
  fun proArrivingFromTheListenerShowsYouHavePro() =
    runTest {
      val viewModel = viewModel()
      val job = viewModel.state.launchIn(this + UnconfinedTestDispatcher(testScheduler))

      source.pro.value = true

      assertTrue(viewModel.state.value.isPro)
      job.cancel()
    }
}

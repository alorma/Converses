package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueCatProDataSourceTest {
  private class FakeGateway(
    var price: String? = "4,99 €",
    var purchaseResult: PurchaseResult = PurchaseResult.Purchased,
    var restoreResult: RestoreResult = RestoreResult.Restored,
  ) : PurchasesGateway {
    var onEntitlement: ((Boolean) -> Unit)? = null

    override fun start(onEntitlement: (Boolean) -> Unit) {
      this.onEntitlement = onEntitlement
    }

    override suspend fun price(): String? = price

    override suspend fun purchase(activity: Activity): PurchaseResult = purchaseResult

    override suspend fun restore(): RestoreResult = restoreResult

    fun send(active: Boolean) = onEntitlement!!(active)
  }

  private val activity = Activity()

  @Test
  fun startsWithTheLastKnownValueBeforeRevenueCatAnswers() {
    val gateway = FakeGateway()
    val source = RevenueCatProDataSource(gateway, initial = true)

    source.start()

    assertTrue(source.isPro.value)
  }

  @Test
  fun followsTheEntitlement() {
    val gateway = FakeGateway()
    val source = RevenueCatProDataSource(gateway)
    source.start()

    gateway.send(true)
    assertTrue(source.isPro.value)

    gateway.send(false)
    assertFalse(source.isPro.value)
  }

  @Test
  fun persistsOnlyChanges() {
    val gateway = FakeGateway()
    val saved = mutableListOf<Boolean>()
    val source = RevenueCatProDataSource(gateway, initial = false, persist = { saved += it })
    source.start()

    gateway.send(false)
    gateway.send(true)
    gateway.send(true)

    assertEquals(listOf(true), saved)
  }

  @Test
  fun doesNotTouchRevenueCatUntilStarted() {
    val gateway = FakeGateway()
    RevenueCatProDataSource(gateway)

    assertNull(gateway.onEntitlement)
  }

  @Test
  fun repositoryFollowsRevenueCat() {
    val gateway = FakeGateway()
    val source = RevenueCatProDataSource(gateway)
    val repository = ProRepository(source)
    source.start()

    gateway.send(true)

    assertTrue(repository.isPro.value)
  }

  @Test
  fun priceComesFromTheGatewayAndIsNullOffline() =
    runTest {
      val gateway = FakeGateway()
      val repository = ProRepository(RevenueCatProDataSource(gateway))

      assertEquals("4,99 €", repository.price())

      gateway.price = null
      assertNull(repository.price())
    }

  @Test
  fun purchaseTurnsProOnWithoutWaitingForTheListener() =
    runTest {
      val saved = mutableListOf<Boolean>()
      val repository = ProRepository(RevenueCatProDataSource(FakeGateway(), persist = { saved += it }))

      assertEquals(PurchaseResult.Purchased, repository.purchase(activity))

      assertTrue(repository.isPro.value)
      assertEquals(listOf(true), saved)
    }

  @Test
  fun purchaseThatDoesNotFinishLeavesProOff() =
    runTest {
      val outcomes =
        listOf(PurchaseResult.Pending, PurchaseResult.Cancelled, PurchaseResult.Offline, PurchaseResult.Failed)
      outcomes.forEach { outcome ->
        val repository = ProRepository(RevenueCatProDataSource(FakeGateway(purchaseResult = outcome)))

        assertEquals(outcome, repository.purchase(activity))
        assertFalse(repository.isPro.value)
      }
    }

  @Test
  fun pendingPurchaseTurnsProOnWhenTheListenerSaysSo() =
    runTest {
      val gateway = FakeGateway(purchaseResult = PurchaseResult.Pending)
      val source = RevenueCatProDataSource(gateway)
      source.start()

      source.purchase(activity)
      gateway.send(true)

      assertTrue(source.isPro.value)
    }

  @Test
  fun restoreWithAPurchaseTurnsProOn() =
    runTest {
      val repository = ProRepository(RevenueCatProDataSource(FakeGateway(restoreResult = RestoreResult.Restored)))

      assertEquals(RestoreResult.Restored, repository.restore())
      assertTrue(repository.isPro.value)
    }

  @Test
  fun restoreWithoutAPurchaseLeavesProOff() =
    runTest {
      val outcomes = listOf(RestoreResult.NothingToRestore, RestoreResult.Offline, RestoreResult.Failed)
      outcomes.forEach { outcome ->
        val repository = ProRepository(RevenueCatProDataSource(FakeGateway(restoreResult = outcome)))

        assertEquals(outcome, repository.restore())
        assertFalse(repository.isPro.value)
      }
    }
}

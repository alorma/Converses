package com.alorma.contactnotes.data.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueCatProDataSourceTest {
  private class FakeGateway : PurchasesGateway {
    var onEntitlement: ((Boolean) -> Unit)? = null

    override fun start(onEntitlement: (Boolean) -> Unit) {
      this.onEntitlement = onEntitlement
    }

    fun send(active: Boolean) = onEntitlement!!(active)
  }

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

    assertEquals(null, gateway.onEntitlement)
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
}

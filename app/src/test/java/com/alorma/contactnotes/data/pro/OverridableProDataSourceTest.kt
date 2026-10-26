package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverridableProDataSourceTest {
  private class FakeRemote : ProDataSource {
    val state = MutableStateFlow(false)
    var started = false
    var purchases = 0

    override val isPro: StateFlow<Boolean> = state

    override fun start() {
      started = true
    }

    override suspend fun price(): String = "4,99 €"

    override suspend fun purchase(activity: Activity): PurchaseResult {
      purchases++
      return PurchaseResult.Cancelled
    }
  }

  private val local = LocalProDataSource()
  private val remote = FakeRemote()

  private fun source(
    initialOverride: Boolean = false,
    persist: (Boolean) -> Unit = {},
  ) = OverridableProDataSource(local, remote, initialOverride, persist, CoroutineScope(UnconfinedTestDispatcher()))

  @Test
  fun withoutOverrideRevenueCatDecides() {
    val source = source()
    local.setPro(true)
    assertFalse(source.isPro.value)

    remote.state.value = true
    assertTrue(source.isPro.value)
  }

  @Test
  fun withOverrideTheLocalSwitchDecides() {
    val source = source(initialOverride = true)
    remote.state.value = true
    assertFalse(source.isPro.value)

    local.setPro(true)
    assertTrue(source.isPro.value)
  }

  @Test
  fun switchingTheOverrideSwitchesTheSourceAndIsPersisted() {
    val saved = mutableListOf<Boolean>()
    val source = source(persist = { saved += it })
    local.setPro(true)

    source.setOverride(true)
    assertTrue(source.isPro.value)

    source.setOverride(false)
    assertFalse(source.isPro.value)
    assertEquals(listOf(true, false), saved)
  }

  @Test
  fun revenueCatStartsEvenWithTheOverrideOn() {
    source(initialOverride = true).start()
    assertTrue(remote.started)
  }

  @Test
  fun purchaseGoesToTheSourceThatDecides() =
    runTest {
      val source = source()
      assertEquals(PurchaseResult.Cancelled, source.purchase(Activity()))
      assertEquals(1, remote.purchases)

      source.setOverride(true)
      assertEquals(PurchaseResult.Purchased, source.purchase(Activity()))
      assertEquals(1, remote.purchases)
      assertTrue(source.isPro.value)
      assertEquals("Debug", source.price())
    }
}

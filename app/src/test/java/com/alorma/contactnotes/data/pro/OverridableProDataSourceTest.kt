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

    override suspend fun purchase(activity: Activity): PurchaseResult {
      purchases++
      return PurchaseResult.Cancelled
    }
  }

  private val remote = FakeRemote()

  private fun source(
    initialOverride: Boolean = false,
    persist: (Boolean) -> Unit = {},
  ) = OverridableProDataSource(remote, initialOverride, persist, CoroutineScope(UnconfinedTestDispatcher()))

  @Test
  fun withoutOverrideRevenueCatDecides() {
    val source = source()
    assertFalse(source.isPro.value)

    remote.state.value = true
    assertTrue(source.isPro.value)
  }

  @Test
  fun overrideIsProWithoutBuying() {
    val source = source(initialOverride = true)
    assertTrue(source.isPro.value)
  }

  @Test
  fun turningTheOverrideOffGoesBackToRevenueCatAndIsPersisted() {
    val saved = mutableListOf<Boolean>()
    val source = source(persist = { saved += it })

    source.setOverride(true)
    assertTrue(source.isPro.value)

    source.setOverride(false)
    assertFalse(source.isPro.value)
    assertEquals(listOf(true, false), saved)
  }

  @Test
  fun aPurchaseStaysProWhateverTheOverride() {
    val source = source()
    remote.state.value = true

    source.setOverride(true)
    source.setOverride(false)

    assertTrue(source.isPro.value)
  }

  @Test
  fun revenueCatStartsEvenWithTheOverrideOn() {
    source(initialOverride = true).start()
    assertTrue(remote.started)
  }

  @Test
  fun purchaseAlwaysGoesToRevenueCat() =
    runTest {
      val source = source(initialOverride = true)
      assertEquals(PurchaseResult.Cancelled, source.purchase(Activity()))
      assertEquals(1, remote.purchases)
    }
}

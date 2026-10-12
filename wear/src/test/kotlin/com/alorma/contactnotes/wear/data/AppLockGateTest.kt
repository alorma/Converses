package com.alorma.contactnotes.wear.data

import com.alorma.contactnotes.shared.WearSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockGateTest {
  private fun stored(appLockActive: Boolean) =
    StoredSnapshot(WearSnapshot(generatedAt = 1, appLockActive = appLockActive, people = emptyList()), syncedAt = 5)

  @Test
  fun lockIsRequiredOnlyWhenTheAppLockIsOnAndTheWatchIsInsecure() {
    assertTrue(isLockRequired(appLockActive = true, deviceSecure = false))
    assertFalse(isLockRequired(appLockActive = true, deviceSecure = true))
    assertFalse(isLockRequired(appLockActive = false, deviceSecure = false))
    assertFalse(isLockRequired(appLockActive = false, deviceSecure = true))
  }

  @Test
  fun anUnverifiableStateIsAssumedActive() {
    assertTrue(isLockRequired(appLockActive = null, deviceSecure = false))
    assertFalse(isLockRequired(appLockActive = null, deviceSecure = true))
  }

  @Test
  fun anInsecureWatchWithTheAppLockOnErasesTheCopyAndRequiresALock() =
    runTest {
      var purges = 0
      val gate = AppLockGate(MutableStateFlow(stored(true)), { false }) { purges++ }

      assertTrue(gate.lockRequired.first())
      assertEquals(1, purges)
    }

  @Test
  fun nothingIsErasedWhenThereIsNothingStored() =
    runTest {
      var purges = 0
      val gate = AppLockGate(MutableStateFlow(null), { false }) { purges++ }

      assertTrue(gate.lockRequired.first())
      assertEquals(0, purges)
    }

  @Test
  fun aSecureWatchOrAnInactiveAppLockKeepsTheCopy() =
    runTest {
      var purges = 0
      assertFalse(AppLockGate(MutableStateFlow(stored(true)), { true }) { purges++ }.lockRequired.first())
      assertFalse(AppLockGate(MutableStateFlow(stored(false)), { false }) { purges++ }.lockRequired.first())
      assertEquals(0, purges)
    }

  @Test
  fun recheckPicksUpALockRemovedWhileTheAppWasAway() =
    runTest {
      var secure = true
      var purges = 0
      val gate = AppLockGate(MutableStateFlow(stored(true)), { secure }) { purges++ }
      assertFalse(gate.lockRequired.first())

      secure = false
      gate.recheck()

      assertTrue(gate.lockRequired.first())
      assertEquals(1, purges)
    }

  @Test
  fun theAppLockSwitchedOnOrOffWhileTheWatchIsOpenIsPickedUpFromTheNextSnapshot() =
    runTest {
      var purges = 0
      val snapshots = MutableStateFlow<StoredSnapshot?>(stored(false))
      val gate = AppLockGate(snapshots, { false }) { purges++ }
      val seen = mutableListOf<Boolean>()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { gate.lockRequired.toList(seen) }
      assertEquals(listOf(false), seen)

      snapshots.value = stored(true)
      assertEquals(listOf(false, true), seen)
      assertEquals(1, purges)

      snapshots.value = stored(false)
      assertEquals(listOf(false, true, false), seen)
      assertEquals(1, purges)
    }
}

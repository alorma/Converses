package com.alorma.contactnotes.data.settings

import com.alorma.contactnotes.testing.FakeSettingsRepository
import com.alorma.contactnotes.testing.RecordingTracker
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockManagerTest {
  private val settings = FakeSettingsRepository()
  private val tracker = RecordingTracker()
  private var clock = 0L

  private fun TestScope.manager() =
    AppLockManager(
      settingsRepository = settings,
      tracker = tracker.eventTracker,
      scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
      now = { clock },
    )

  private fun locked(relock: Boolean) = "app_locked" to mapOf("reason" to if (relock) "relock" else "cold_start")

  private fun background(
    manager: AppLockManager,
    seconds: Long,
  ) {
    manager.onAppStopped()
    clock += seconds * 1000
    manager.onAppStarted()
  }

  @Test
  fun aColdStartWithTheLockOnIsTrackedOnce() =
    runTest {
      settings.state.update { it.copy(appLock = true) }

      val manager = manager()

      assertTrue(manager.showLock.value)
      assertEquals(listOf(locked(relock = false)), tracker.recordedEvents())
    }

  @Test
  fun withoutTheLockNothingIsTracked() =
    runTest {
      val manager = manager()
      background(manager, seconds = 3_600)

      assertFalse(manager.showLock.value)
      assertEquals(emptyList<Pair<String, Map<String, String>>>(), tracker.recordedEvents())
    }

  @Test
  fun aRelockAfterTheBackgroundIsTrackedAsRelock() =
    runTest {
      settings.state.update { it.copy(appLock = true, relockAfterSeconds = 30) }
      val manager = manager()
      manager.unlock()

      background(manager, seconds = 10)
      assertFalse(manager.showLock.value)

      background(manager, seconds = 30)
      assertTrue(manager.showLock.value)
      assertEquals(listOf(locked(relock = false), locked(relock = true)), tracker.recordedEvents())
    }

  @Test
  fun turningTheLockOnInSettingsIsNotALock() =
    runTest {
      val manager = manager()

      // Ajustos: desbloqueja abans que el DataStore desi `appLock`
      manager.unlock()
      settings.setAppLock(true)

      assertFalse(manager.showLock.value)
      assertEquals(emptyList<Pair<String, Map<String, String>>>(), tracker.recordedEvents())
    }

  @Test
  fun anUnprotectedDeviceIsTrackedAndUnlocks() =
    runTest {
      settings.state.update { it.copy(appLock = true) }
      val manager = manager()

      manager.onDeviceUnprotected()

      assertFalse(manager.showLock.value)
      assertEquals(
        listOf(locked(relock = false), "app_lock_disabled" to mapOf("reason" to "device_unprotected")),
        tracker.recordedEvents(),
      )
    }
}

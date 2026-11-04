package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.testing.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentManagerTest {
  private val settings = FakeSettingsRepository()
  private val scope = TestScope(UnconfinedTestDispatcher())

  @Test
  fun withoutAChoiceThereIsNoConsent() =
    scope.runTest {
      val manager = ConsentManager(settings, backgroundScope)

      assertNull(manager.consent.value)
      assertFalse(manager.hasConsent())
    }

  @Test
  fun readsTheStoredChoiceRightAway() =
    scope.runTest {
      settings.setAnalyticsConsent(true)

      val manager = ConsentManager(settings, backgroundScope)

      assertTrue(manager.hasConsent())
    }

  @Test
  fun followsChangesAndPersistsThem() =
    scope.runTest {
      val manager = ConsentManager(settings, backgroundScope)

      manager.setConsent(true)
      assertTrue(manager.hasConsent())
      assertEquals(true, settings.state.value.analyticsConsent)

      manager.setConsent(false)
      assertFalse(manager.hasConsent())
      assertEquals(false, settings.state.value.analyticsConsent)
    }

  @Test
  fun eventTrackerStopsRemoteTrackersWhenConsentIsWithdrawn() =
    scope.runTest {
      val manager = ConsentManager(settings, backgroundScope)
      val remote = CountingRemoteTracker()
      val tracker = EventTracker(listOf(remote), manager::hasConsent)

      tracker.trackEvent(object : Event("e", 1) {})
      manager.setConsent(true)
      tracker.trackEvent(object : Event("e", 1) {})
      manager.setConsent(false)
      tracker.trackEvent(object : Event("e", 1) {})

      assertEquals(1, remote.events)
    }

  private class CountingRemoteTracker : Tracker {
    var events = 0
    override val isRemote = true

    override fun trackScreen(screen: Screen) = Unit

    override fun trackEvent(event: Event) {
      events++
    }

    override fun trackAction(action: Action) = Unit

    override fun trackError(
      error: Throwable,
      where: String,
    ) = Unit
  }
}

package com.alorma.contactnotes.ui.settings

import com.alorma.contactnotes.data.tracking.Action
import com.alorma.contactnotes.data.tracking.ConsentManager
import com.alorma.contactnotes.data.tracking.Event
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.Screen
import com.alorma.contactnotes.data.tracking.Tracker
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.FakeSettingsRepository
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  @get:Rule val main = MainDispatcherRule()

  @Test
  fun everyChangeReachesTheRepositoryAndComesBackInTheState() =
    runTest {
      val repository = FakeSettingsRepository()
      val tracker = RecordingTracker()
      val vm =
        SettingsViewModel(
          repository,
          FakeDb().repository,
          tracker.eventTracker,
          ConsentManager(repository, backgroundScope),
        )
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.settings.collect {} }

      assertFalse(vm.settings.value.dynamicColor)

      vm.onThemeMode(ThemeMode.Dark)
      vm.onDynamicColor(true)
      vm.onPersonColorTheme(false)
      vm.onHapticFeedback(false)
      vm.onAppLock(true)
      vm.onRelockAfter(300)

      val settings = vm.settings.value
      assertEquals(ThemeMode.Dark, settings.themeMode)
      assertTrue(settings.dynamicColor)
      assertFalse(settings.personColorTheme)
      assertFalse(settings.hapticFeedback)
      assertTrue(settings.appLock)
      assertEquals(300, settings.relockAfterSeconds)
      assertEquals(
        listOf(
          "theme" to "dark",
          "dynamic_color" to "true",
          "person_color" to "false",
          "haptics" to "false",
          "app_lock" to "true",
          "relock_after" to "300",
        ),
        tracker.recordedActions().map { (name, props) ->
          assertEquals("setting_changed", name)
          props.getValue("setting") to props.getValue("value")
        },
      )
    }

  @Test
  fun analyticsSwitchSavesTheConsentAndTracksTheChange() =
    runTest(UnconfinedTestDispatcher()) {
      val repository = FakeSettingsRepository()
      val tracker = RecordingTracker()
      val vm =
        SettingsViewModel(
          repository,
          FakeDb().repository,
          tracker.eventTracker,
          ConsentManager(repository, backgroundScope),
        )

      vm.onAnalytics(true)
      assertEquals(true, repository.state.value.analyticsConsent)
      vm.onAnalytics(false)
      assertEquals(false, repository.state.value.analyticsConsent)

      assertEquals(
        listOf(mapOf("setting" to "analytics", "value" to "true"), mapOf("setting" to "analytics", "value" to "false")),
        tracker.recordedActions().map { it.second },
      )
    }

  @Test
  fun turningAnalyticsOffStaysOnThePhoneAndTurningThemOnIsSent() =
    runTest(UnconfinedTestDispatcher()) {
      val repository = FakeSettingsRepository()
      repository.setAnalyticsConsent(false)
      val consent = ConsentManager(repository, backgroundScope)
      val remote = RemoteActions()
      val eventTracker = EventTracker(listOf(remote), consent.consent, backgroundScope)
      val vm = SettingsViewModel(repository, FakeDb().repository, eventTracker, consent)

      vm.onAnalytics(true)
      vm.onAnalytics(false)

      assertEquals(listOf("true"), remote.values)
    }

  private class RemoteActions : Tracker {
    override val isRemote = true
    val values = mutableListOf<String>()

    override fun trackScreen(screen: Screen) = Unit

    override fun trackEvent(event: Event) = Unit

    override fun trackAction(action: Action) {
      values += action.eventProperties.getValue("value")
    }

    override fun trackError(
      error: Throwable,
      where: String,
      properties: Map<String, String>,
    ) = Unit
  }
}

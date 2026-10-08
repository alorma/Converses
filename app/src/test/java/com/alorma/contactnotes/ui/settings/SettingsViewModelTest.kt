package com.alorma.contactnotes.ui.settings

import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeSettingsRepository : SettingsRepository {
  val state = MutableStateFlow(AppSettings())
  override val settings: Flow<AppSettings> = state

  override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

  override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }

  override suspend fun setPersonColorTheme(enabled: Boolean) = state.update { it.copy(personColorTheme = enabled) }

  override suspend fun setHapticFeedback(enabled: Boolean) = state.update { it.copy(hapticFeedback = enabled) }

  override suspend fun setAppLock(enabled: Boolean) = state.update { it.copy(appLock = enabled) }

  override suspend fun setRelockAfterSeconds(seconds: Int) = state.update { it.copy(relockAfterSeconds = seconds) }

  override suspend fun setOnboardingDone(done: Boolean) = state.update { it.copy(onboardingDone = done) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  @get:Rule val main = MainDispatcherRule()

  @Test
  fun everyChangeReachesTheRepositoryAndComesBackInTheState() = runTest {
    val repository = FakeSettingsRepository()
    val vm = SettingsViewModel(repository)
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
  }
}

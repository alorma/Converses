package com.alorma.contactnotes.ui.settings

import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.testing.FakeSettingsRepository
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

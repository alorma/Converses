package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository : SettingsRepository {
  val state = MutableStateFlow(AppSettings())
  override val settings: Flow<AppSettings> = state

  override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

  override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }

  override suspend fun setPersonColorTheme(enabled: Boolean) = state.update { it.copy(personColorTheme = enabled) }

  override suspend fun setHapticFeedback(enabled: Boolean) = state.update { it.copy(hapticFeedback = enabled) }

  override suspend fun setAppLock(enabled: Boolean) = state.update { it.copy(appLock = enabled) }

  override suspend fun setRelockAfterSeconds(seconds: Int) = state.update { it.copy(relockAfterSeconds = seconds) }

  override suspend fun setOnboardingDone(done: Boolean) = state.update { it.copy(onboardingDone = done) }

  override suspend fun setChannelsNoticePending(pending: Boolean) = state.update { it.copy(channelsNoticePending = pending) }
}

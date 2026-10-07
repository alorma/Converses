package com.alorma.contactnotes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val repository: SettingsRepository,
) : ViewModel() {
  val settings: StateFlow<AppSettings> = repository.settings
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

  fun onThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

  fun onDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }

  fun onPersonColorTheme(enabled: Boolean) = viewModelScope.launch { repository.setPersonColorTheme(enabled) }

  fun onAppLock(enabled: Boolean) = viewModelScope.launch { repository.setAppLock(enabled) }

  fun onRelockAfter(seconds: Int) = viewModelScope.launch { repository.setRelockAfterSeconds(seconds) }
}

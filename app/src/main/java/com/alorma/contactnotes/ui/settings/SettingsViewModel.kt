package com.alorma.contactnotes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.data.tracking.ConsentManager
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.tracking.Actions
import com.alorma.contactnotes.ui.tracking.ToggleSetting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val repository: SettingsRepository,
  conversesRepository: ConversesRepository,
  private val tracker: EventTracker,
  private val consent: ConsentManager,
) : ViewModel() {
  val settings: StateFlow<AppSettings> =
    repository.settings
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

  /** Nombre de canals propis, per a la fila d'Ajustos › Canals. */
  val customChannelCount: StateFlow<Int> =
    conversesRepository
      .observeCustomChannels()
      .map { it.size }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

  fun onThemeMode(mode: ThemeMode) =
    viewModelScope.launch {
      tracker.trackAction(Actions.SettingChanged.theme(mode))
      repository.setThemeMode(mode)
    }

  fun onDynamicColor(enabled: Boolean) =
    viewModelScope.launch {
      toggled(ToggleSetting.DYNAMIC_COLOR, enabled)
      repository.setDynamicColor(enabled)
    }

  fun onPersonColorTheme(enabled: Boolean) =
    viewModelScope.launch {
      toggled(ToggleSetting.PERSON_COLOR, enabled)
      repository.setPersonColorTheme(enabled)
    }

  fun onHapticFeedback(enabled: Boolean) =
    viewModelScope.launch {
      toggled(ToggleSetting.HAPTICS, enabled)
      repository.setHapticFeedback(enabled)
    }

  /** Només arriba aquí després d'autenticar-se. */
  fun onAppLock(enabled: Boolean) =
    viewModelScope.launch {
      toggled(ToggleSetting.APP_LOCK, enabled)
      repository.setAppLock(enabled)
    }

  /**
   * Estadístiques d'ús. Es registra després de desar-ho: activant-les, el canvi ja surt; desactivant-les, es queda
   * al mòbil.
   */
  fun onAnalytics(enabled: Boolean) =
    viewModelScope.launch {
      consent.saveConsent(enabled)
      toggled(ToggleSetting.ANALYTICS, enabled)
    }

  fun onRelockAfter(seconds: Int) =
    viewModelScope.launch {
      tracker.trackAction(Actions.SettingChanged.relockAfter(seconds))
      repository.setRelockAfterSeconds(seconds)
    }

  private fun toggled(
    setting: ToggleSetting,
    enabled: Boolean,
  ) = tracker.trackAction(Actions.SettingChanged.toggle(setting, enabled))
}

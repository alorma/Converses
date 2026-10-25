package com.alorma.contactnotes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.contacts.ContactsPermission
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estat de la secció «Contactes» d'Ajustos (`docs/contacts/disseny.md`, 1c i 5h). */
sealed interface ContactsSettingsState {
  /** Sense permís i sense haver vist l'explicació. */
  data object NeverOn : ContactsSettingsState

  /** Permís donat; [linkedCount] són les persones amb vincle (també protegides i arxivades). */
  data class On(
    val linkedCount: Int,
  ) : ContactsSettingsState

  /** Sense permís després d'haver-lo demanat. */
  data object Denied : ContactsSettingsState
}

class SettingsViewModel(
  private val repository: SettingsRepository,
  conversesRepository: ConversesRepository,
  private val contactsPermission: ContactsPermission,
) : ViewModel() {
  val settings: StateFlow<AppSettings> =
    repository.settings
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

  private val permissionGranted = MutableStateFlow(contactsPermission.isGranted())

  val contacts: StateFlow<ContactsSettingsState> =
    combine(
      permissionGranted,
      repository.settings,
      conversesRepository.observeLinkedContactsCount(),
    ) { granted, settings, linked ->
      when {
        granted -> ContactsSettingsState.On(linked)
        settings.contactsExplained -> ContactsSettingsState.Denied
        else -> ContactsSettingsState.NeverOn
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsSettingsState.NeverOn)

  /** El permís es pot haver donat o retirat des del diàleg o els ajustos de l'app. */
  fun onContactsPermissionChanged() {
    permissionGranted.value = contactsPermission.isGranted()
  }

  fun onContactsExplained() = viewModelScope.launch { repository.setContactsExplained(true) }

  fun onThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

  fun onDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }

  fun onPersonColorTheme(enabled: Boolean) = viewModelScope.launch { repository.setPersonColorTheme(enabled) }

  fun onHapticFeedback(enabled: Boolean) = viewModelScope.launch { repository.setHapticFeedback(enabled) }

  fun onAppLock(enabled: Boolean) = viewModelScope.launch { repository.setAppLock(enabled) }

  fun onRelockAfter(seconds: Int) = viewModelScope.launch { repository.setRelockAfterSeconds(seconds) }
}

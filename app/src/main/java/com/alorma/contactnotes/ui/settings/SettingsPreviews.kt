package com.alorma.contactnotes.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.flags.FeatureState
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Composable
private fun SettingsPreview(
  settings: AppSettings,
  canUnlock: Boolean,
) {
  ConversesPreviewTheme {
    SettingsContent(
      settings = settings,
      canUnlock = canUnlock,
      languageName = "English",
      versionName = "1.0 (1)",
      onBack = {},
      onThemeMode = {},
      onDynamicColor = {},
      onPersonColorTheme = {},
      onHapticFeedback = {},
      onAppLockChange = {},
      onRelockAfter = {},
      onOpenLanguage = {},
      onOpenSecuritySetup = {},
      onOpenPrivacyPolicy = {},
      onOpenFeedback = {},
    )
  }
}

/** Color dinàmic apagat (tema Grafit), bloqueig de l'app activat i «Torna a bloquejar». */
@Preview
@Composable
internal fun SettingsAppLockPreview() {
  SettingsPreview(
    settings = AppSettings(dynamicColor = false, appLock = true),
    canUnlock = true,
  )
}

/** Dispositiu sense bloqueig de pantalla: surt «Configura la seguretat». */
@Preview
@Composable
internal fun SettingsNoLockPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = false)
}

/** Hi ha un rellotge emparellat: surt l'interruptor de notificacions del rellotge. */
@Preview
@Composable
internal fun SettingsWatchPreview() {
  ConversesPreviewTheme {
    SettingsContent(
      settings = AppSettings(),
      canUnlock = true,
      languageName = "English",
      versionName = "1.0 (1)",
      onBack = {},
      onThemeMode = {},
      onDynamicColor = {},
      onPersonColorTheme = {},
      onHapticFeedback = {},
      onAppLockChange = {},
      onRelockAfter = {},
      onOpenLanguage = {},
      onOpenSecuritySetup = {},
      onOpenPrivacyPolicy = {},
      onOpenFeedback = {},
      showWatchNotifications = true,
      watchNotifications = false,
    )
  }
}

@Composable
private fun SettingsContactsPreview(
  contacts: ContactsSettingsState,
  personColor: FeatureState = FeatureState.Unlocked,
) {
  ConversesPreviewTheme {
    SettingsContent(
      settings = AppSettings(),
      canUnlock = true,
      languageName = "English",
      versionName = "1.0 (1)",
      onBack = {},
      onThemeMode = {},
      onDynamicColor = {},
      onPersonColorTheme = {},
      onHapticFeedback = {},
      onAppLockChange = {},
      onRelockAfter = {},
      onOpenLanguage = {},
      onOpenSecuritySetup = {},
      onOpenPrivacyPolicy = {},
      onOpenFeedback = {},
      contacts = contacts,
      personColor = personColor,
    )
  }
}

/** Contactes: mai activat (1c, 5h). */
@Preview
@Composable
internal fun SettingsContactsNeverOnPreview() {
  SettingsContactsPreview(ContactsSettingsState.NeverOn)
}

/** Contactes: activat amb tres files. */
@Preview
@Composable
internal fun SettingsContactsOnPreview() {
  SettingsContactsPreview(ContactsSettingsState.On(linkedCount = 3))
}

/** Usuari gratuït amb els flags oberts: Contactes (5h) i «Person color» bloquejats. */
@Preview
@Composable
internal fun SettingsContactsLockedPreview() {
  SettingsContactsPreview(ContactsSettingsState.Locked, personColor = FeatureState.Locked)
}

/** Contactes: permís denegat. */
@Preview
@Composable
internal fun SettingsContactsDeniedPreview() {
  SettingsContactsPreview(ContactsSettingsState.Denied)
}

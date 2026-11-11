package com.alorma.contactnotes.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.sync.SyncIndicatorState

@Composable
private fun SettingsPreview(
  settings: AppSettings,
  canUnlock: Boolean,
  isPro: Boolean = false,
  syncState: SyncIndicatorState? = null,
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
      onAnalyticsChange = {},
      onOpenLanguage = {},
      onOpenSecuritySetup = {},
      onOpenPrivacyPolicy = {},
      onOpenFeedback = {},
      isPro = isPro,
      syncState = syncState,
    )
  }
}

/** Amb Pro: la targeta de dalt és normal i diu «Activat» (1c). */
@Preview
@Composable
internal fun SettingsProPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = true, isPro = true)
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

/** Sense Pro (11e): «Color de la persona» porta PRO al títol i un chevron; tota la fila obre la pantalla de Pro. */
@Preview
@Composable
internal fun SettingsPersonColorLockedPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = true)
}

/** Ajustos › Privacitat amb les estadístiques activades (1c). */
@Preview(heightDp = 2000)
@Composable
internal fun SettingsAnalyticsOnPreview() {
  SettingsPreview(settings = AppSettings(analyticsConsent = true), canUnlock = true, isPro = true)
}

/** Amb SyncFlag encès (13b): la secció «Compte» sota la targeta de Pro, sense sessió. */
@Preview
@Composable
internal fun SettingsSyncOffPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = true, syncState = SyncIndicatorState.SignedOut)
}

@Preview
@Composable
internal fun SettingsSyncOnPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = true, syncState = SyncIndicatorState.On("B"))
}

@Preview
@Composable
internal fun SettingsSyncKeyPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = true, syncState = SyncIndicatorState.NeedsKey("B"))
}

package com.alorma.contactnotes.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.ThemeColor

@Composable
private fun SettingsPreview(settings: AppSettings, canUnlock: Boolean) {
  ConversesPreviewTheme {
    SettingsContent(
      settings = settings,
      canUnlock = canUnlock,
      languageName = "English",
      versionName = "1.0",
      onBack = {},
      onThemeMode = {},
      onDynamicColor = {},
      onThemeColor = {},
      onPersonColorTheme = {},
      onAppLockChange = {},
      onRelockAfter = {},
      onOpenLanguage = {},
      onOpenSecuritySetup = {},
      onOpenPrivacyPolicy = {},
    )
  }
}

/** Color dinàmic apagat (surt el selector de color), bloqueig de l'app activat i «Torna a bloquejar». */
@Preview
@Composable
internal fun SettingsAppLockPreview() {
  SettingsPreview(
    settings = AppSettings(dynamicColor = false, themeColor = ThemeColor.Mint, appLock = true),
    canUnlock = true,
  )
}

/** Dispositiu sense bloqueig de pantalla: surt «Configura la seguretat». */
@Preview
@Composable
internal fun SettingsNoLockPreview() {
  SettingsPreview(settings = AppSettings(), canUnlock = false)
}

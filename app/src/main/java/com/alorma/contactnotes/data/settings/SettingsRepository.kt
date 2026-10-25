package com.alorma.contactnotes.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.System,
  val dynamicColor: Boolean = false,
  val personColorTheme: Boolean = true,
  val hapticFeedback: Boolean = true,
  val appLock: Boolean = false,
  val relockAfterSeconds: Int = 60,
  val onboardingDone: Boolean = false,
  /** «No m'ho tornis a demanar» de la proposta d'instal·lar l'app al rellotge. Preferència local, no Room. */
  val watchInstallDismissed: Boolean = false,
  /** Ja s'ha vist l'explicació del permís de Contactes (5b): no es torna a mostrar abans del diàleg del sistema. */
  val contactsExplained: Boolean = false,
)

interface SettingsRepository {
  val settings: Flow<AppSettings>

  suspend fun setThemeMode(mode: ThemeMode)

  suspend fun setDynamicColor(enabled: Boolean)

  suspend fun setPersonColorTheme(enabled: Boolean)

  suspend fun setHapticFeedback(enabled: Boolean)

  suspend fun setAppLock(enabled: Boolean)

  suspend fun setRelockAfterSeconds(seconds: Int)

  suspend fun setOnboardingDone(done: Boolean)

  suspend fun setWatchInstallDismissed(dismissed: Boolean)

  suspend fun setContactsExplained(explained: Boolean)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(
  context: Context,
) : SettingsRepository {
  private val dataStore = context.applicationContext.settingsDataStore

  override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

  override suspend fun setThemeMode(mode: ThemeMode) = write { it[ThemeModeKey] = mode.name.uppercase() }

  override suspend fun setDynamicColor(enabled: Boolean) = write { it[DynamicColorKey] = enabled }

  override suspend fun setPersonColorTheme(enabled: Boolean) = write { it[PersonColorThemeKey] = enabled }

  override suspend fun setHapticFeedback(enabled: Boolean) = write { it[HapticFeedbackKey] = enabled }

  override suspend fun setAppLock(enabled: Boolean) = write { it[AppLockKey] = enabled }

  override suspend fun setRelockAfterSeconds(seconds: Int) = write { it[RelockAfterKey] = seconds }

  override suspend fun setOnboardingDone(done: Boolean) = write { it[OnboardingDoneKey] = done }

  override suspend fun setWatchInstallDismissed(dismissed: Boolean) = write { it[WatchInstallDismissedKey] = dismissed }

  override suspend fun setContactsExplained(explained: Boolean) = write { it[ContactsExplainedKey] = explained }

  private suspend fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
    dataStore.edit { block(it) }
  }

  private fun Preferences.toSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
      themeMode = enumOrDefault(this[ThemeModeKey], defaults.themeMode),
      dynamicColor = this[DynamicColorKey] ?: defaults.dynamicColor,
      personColorTheme = this[PersonColorThemeKey] ?: defaults.personColorTheme,
      hapticFeedback = this[HapticFeedbackKey] ?: defaults.hapticFeedback,
      appLock = this[AppLockKey] ?: defaults.appLock,
      relockAfterSeconds = this[RelockAfterKey] ?: defaults.relockAfterSeconds,
      onboardingDone = this[OnboardingDoneKey] ?: defaults.onboardingDone,
      watchInstallDismissed = this[WatchInstallDismissedKey] ?: defaults.watchInstallDismissed,
      contactsExplained = this[ContactsExplainedKey] ?: defaults.contactsExplained,
    )
  }

  private companion object {
    val ThemeModeKey = stringPreferencesKey("theme_mode")
    val DynamicColorKey = booleanPreferencesKey("dynamic_color")
    val PersonColorThemeKey = booleanPreferencesKey("person_color_theme")
    val HapticFeedbackKey = booleanPreferencesKey("haptic_feedback")
    val AppLockKey = booleanPreferencesKey("app_lock")
    val RelockAfterKey = intPreferencesKey("relock_after")
    val OnboardingDoneKey = booleanPreferencesKey("onboarding_done")
    val WatchInstallDismissedKey = booleanPreferencesKey("watch_install_dismissed")
    val ContactsExplainedKey = booleanPreferencesKey("contacts_explained")
  }
}

private inline fun <reified E : Enum<E>> enumOrDefault(
  stored: String?,
  default: E,
): E = enumValues<E>().firstOrNull { it.name.equals(stored, ignoreCase = true) } ?: default

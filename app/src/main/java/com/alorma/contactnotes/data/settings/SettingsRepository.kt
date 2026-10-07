package com.alorma.contactnotes.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.contactnotes.designsystem.theme.ThemeColor
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.System,
  val dynamicColor: Boolean = true,
  val themeColor: ThemeColor = ThemeColor.Lavender,
  val personColorTheme: Boolean = true,
  val appLock: Boolean = false,
  val relockAfterSeconds: Int = 60,
  val onboardingDone: Boolean = false,
)

interface SettingsRepository {
  val settings: Flow<AppSettings>

  suspend fun setThemeMode(mode: ThemeMode)

  suspend fun setDynamicColor(enabled: Boolean)

  suspend fun setThemeColor(color: ThemeColor)

  suspend fun setPersonColorTheme(enabled: Boolean)

  suspend fun setAppLock(enabled: Boolean)

  suspend fun setRelockAfterSeconds(seconds: Int)

  suspend fun setOnboardingDone(done: Boolean)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(context: Context) : SettingsRepository {
  private val dataStore = context.applicationContext.settingsDataStore

  override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

  override suspend fun setThemeMode(mode: ThemeMode) = write { it[ThemeModeKey] = mode.name.uppercase() }

  override suspend fun setDynamicColor(enabled: Boolean) = write { it[DynamicColorKey] = enabled }

  override suspend fun setThemeColor(color: ThemeColor) = write { it[ThemeColorKey] = color.name.uppercase() }

  override suspend fun setPersonColorTheme(enabled: Boolean) = write { it[PersonColorThemeKey] = enabled }

  override suspend fun setAppLock(enabled: Boolean) = write { it[AppLockKey] = enabled }

  override suspend fun setRelockAfterSeconds(seconds: Int) = write { it[RelockAfterKey] = seconds }

  override suspend fun setOnboardingDone(done: Boolean) = write { it[OnboardingDoneKey] = done }

  private suspend fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
    dataStore.edit { block(it) }
  }

  private fun Preferences.toSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
      themeMode = enumOrDefault(this[ThemeModeKey], defaults.themeMode),
      dynamicColor = this[DynamicColorKey] ?: defaults.dynamicColor,
      themeColor = enumOrDefault(this[ThemeColorKey], defaults.themeColor),
      personColorTheme = this[PersonColorThemeKey] ?: defaults.personColorTheme,
      appLock = this[AppLockKey] ?: defaults.appLock,
      relockAfterSeconds = this[RelockAfterKey] ?: defaults.relockAfterSeconds,
      onboardingDone = this[OnboardingDoneKey] ?: defaults.onboardingDone,
    )
  }

  private companion object {
    val ThemeModeKey = stringPreferencesKey("theme_mode")
    val DynamicColorKey = booleanPreferencesKey("dynamic_color")
    val ThemeColorKey = stringPreferencesKey("theme_color")
    val PersonColorThemeKey = booleanPreferencesKey("person_color_theme")
    val AppLockKey = booleanPreferencesKey("app_lock")
    val RelockAfterKey = intPreferencesKey("relock_after")
    val OnboardingDoneKey = booleanPreferencesKey("onboarding_done")
  }
}

private inline fun <reified E : Enum<E>> enumOrDefault(stored: String?, default: E): E =
  enumValues<E>().firstOrNull { it.name.equals(stored, ignoreCase = true) } ?: default

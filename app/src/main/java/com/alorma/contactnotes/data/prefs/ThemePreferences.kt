package com.alorma.contactnotes.data.prefs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.alorma.contactnotes.designsystem.theme.ColorStyle
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.russhwolf.settings.Settings

interface ThemePreferences {
  val themeMode: MutableState<ThemeMode>
  val colorStyle: MutableState<ColorStyle>

  fun setThemeMode(mode: ThemeMode)

  fun setColorStyle(style: ColorStyle)
}

class ThemePreferencesImpl(
  private val settings: Settings,
) : ThemePreferences {
  override val themeMode = mutableStateOf(settings.readEnum(KEY_THEME_MODE, ThemeMode.System))
  override val colorStyle = mutableStateOf(settings.readEnum(KEY_COLOR_STYLE, ColorStyle.Tinted))

  override fun setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
    settings.putString(KEY_THEME_MODE, mode.name)
  }

  override fun setColorStyle(style: ColorStyle) {
    colorStyle.value = style
    settings.putString(KEY_COLOR_STYLE, style.name)
  }

  private companion object {
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_COLOR_STYLE = "color_style"
  }
}

private inline fun <reified E : Enum<E>> Settings.readEnum(key: String, default: E): E =
  getStringOrNull(key)?.let { saved -> enumValues<E>().firstOrNull { it.name == saved } } ?: default

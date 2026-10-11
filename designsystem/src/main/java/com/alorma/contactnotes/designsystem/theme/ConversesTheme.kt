package com.alorma.contactnotes.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

enum class ThemeMode { System, Light, Dark }

/** Llavor i estil del tema Grafit, el que es fa servir amb el color dinàmic desactivat. */
internal val GraphiteSeed = Color.Black
internal val GraphiteStyle = PaletteStyle.Monochrome

internal val LocalIsDarkTheme = compositionLocalOf { false }

val isDarkTheme: Boolean
  @Composable @ReadOnlyComposable
  get() = LocalIsDarkTheme.current

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesTheme(
  themeMode: ThemeMode = ThemeMode.System,
  dynamicColor: Boolean = true,
  personColorTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.System -> isSystemInDarkTheme()
      ThemeMode.Light -> false
      ThemeMode.Dark -> true
    }
  val context = LocalContext.current
  val dynamicSeed = remember(context) { dynamicLightColorScheme(context).primary }
  // Sense color dinàmic, el tema Grafit: grisos purs, amb `primary` negre en clar i blanc en fosc.
  val colorScheme =
    rememberDynamicColorScheme(
      seedColor = if (dynamicColor) dynamicSeed else GraphiteSeed,
      isDark = isDark,
      style = if (dynamicColor) PaletteStyle.Neutral else GraphiteStyle,
    ).withFixedError(isDark)

  CompositionLocalProvider(LocalIsDarkTheme provides isDark, LocalPersonColorTheme provides personColorTheme) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = ConversesTypography,
      content = content,
    )
  }
}

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
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

enum class ThemeMode { System, Light, Dark }

/** `Tinted` és el "tenyit" (Neutral); `Pure` és el "pur" (Monochrome). */
enum class ColorStyle(internal val palette: PaletteStyle) {
  Tinted(PaletteStyle.Neutral),
  Pure(PaletteStyle.Monochrome),
}

private val LocalIsDarkTheme = compositionLocalOf { false }

val isDarkTheme: Boolean
  @Composable @ReadOnlyComposable get() = LocalIsDarkTheme.current

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesTheme(
  themeMode: ThemeMode = ThemeMode.System,
  colorStyle: ColorStyle = ColorStyle.Tinted,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
  }
  val context = LocalContext.current
  val seed = remember(context) { dynamicLightColorScheme(context).primary }
  val colorScheme = rememberDynamicColorScheme(
    seedColor = seed,
    isDark = isDark,
    style = colorStyle.palette,
  ).withFixedError(isDark)

  CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = ConversesTypography,
      content = content,
    )
  }
}

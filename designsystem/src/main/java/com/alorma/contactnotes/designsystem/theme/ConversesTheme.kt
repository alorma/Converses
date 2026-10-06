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

/** Color del tema quan el color dinàmic està desactivat: mostra pastel i llavor de l'esquema. */
enum class ThemeColor(val swatch: Color, val seed: Color) {
  Lavender(Color(0xFFDCD3F5), Color(0xFF6B55B5)),
  Mint(Color(0xFFC9EBD6), Color(0xFF2F7A52)),
  Peach(Color(0xFFFFD6BF), Color(0xFFB5602E)),
  Sky(Color(0xFFCBE3F7), Color(0xFF2D6FA3)),
  Rose(Color(0xFFF7CFDD), Color(0xFFB04670)),
  Lemon(Color(0xFFF1E9A8), Color(0xFF8A7A12)),
  Sand(Color(0xFFE8DDCB), Color(0xFF86704C)),
}

internal val LocalIsDarkTheme = compositionLocalOf { false }

val isDarkTheme: Boolean
  @Composable @ReadOnlyComposable get() = LocalIsDarkTheme.current

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesTheme(
  themeMode: ThemeMode = ThemeMode.System,
  dynamicColor: Boolean = true,
  themeColor: ThemeColor = ThemeColor.Lavender,
  personColorTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
  }
  val context = LocalContext.current
  val dynamicSeed = remember(context) { dynamicLightColorScheme(context).primary }
  val colorScheme = rememberDynamicColorScheme(
    seedColor = if (dynamicColor) dynamicSeed else themeColor.seed,
    isDark = isDark,
    style = if (dynamicColor) PaletteStyle.Neutral else PaletteStyle.TonalSpot,
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

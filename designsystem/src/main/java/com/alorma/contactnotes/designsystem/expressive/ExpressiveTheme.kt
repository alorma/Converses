package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.alorma.contactnotes.designsystem.theme.LocalAppColorScheme
import com.alorma.contactnotes.designsystem.theme.LocalDynamicSeed
import com.alorma.contactnotes.designsystem.theme.isDarkTheme

/**
 * Tema de l'app expressiva. Va dins de `ConversesTheme` (que li dóna la tipografia, les formes i si és fosc):
 * hi posa els colors expressius i en construeix un esquema de Material complet; amb color dinàmic
 * (`LocalDynamicSeed`) la paleta surt del fons de pantalla ([dynamicExpressive]). Només
 * els colors d'error vénen de l'esquema de l'app. També l'ofereix com a esquema de l'app (`LocalAppColorScheme`)
 * perquè `AppColors` hi torni des de dins d'una persona.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
  val isDark = isDarkTheme
  val seed = LocalDynamicSeed.current
  val base = MaterialTheme.colorScheme
  // Amb color dinàmic la paleta surt del fons de pantalla; sense, és la crema fixa.
  val dynamic = remember(seed, isDark) { seed?.let { dynamicExpressive(it, isDark) } }
  val colors = dynamic?.colors ?: if (isDark) DarkExpressiveColors else LightExpressiveColors
  val colorScheme =
    remember(colors, dynamic, isDark, base.error, base.onError, base.errorContainer, base.onErrorContainer) {
      expressiveColorScheme(colors, isDark, base, dynamic)
    }
  CompositionLocalProvider(
    LocalExpressiveColors provides colors,
    LocalAppColorScheme provides colorScheme,
  ) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = MaterialTheme.typography,
      shapes = MaterialTheme.shapes,
      content = content,
    )
  }
}

/**
 * Esquema complet amb tots els rols explícits, inclosos els «fixed» ([withExpressiveFixed]); només els d'error
 * vénen de [base].
 */
private fun expressiveColorScheme(
  colors: ExpressiveColors,
  isDark: Boolean,
  base: ColorScheme,
  dynamic: DynamicExpressive?,
): ColorScheme =
  (if (isDark) darkColorScheme() else lightColorScheme())
    .copy(
      primary = colors.ink,
      onPrimary = colors.background,
      primaryContainer = colors.accent,
      onPrimaryContainer = colors.onAccent,
      inversePrimary = colors.accent,
      secondary = colors.inkSecondary,
      onSecondary = colors.background,
      secondaryContainer = colors.accent,
      onSecondaryContainer = colors.onAccent,
      tertiary = colors.inkSecondary,
      onTertiary = colors.background,
      tertiaryContainer = colors.neutral,
      onTertiaryContainer = colors.ink,
      background = colors.background,
      onBackground = colors.ink,
      surface = colors.background,
      onSurface = colors.ink,
      surfaceVariant = colors.neutral,
      onSurfaceVariant = colors.inkSecondary,
      surfaceTint = colors.ink,
      inverseSurface = colors.ink,
      inverseOnSurface = colors.background,
      outline = colors.outline,
      outlineVariant = colors.outlineVariant,
      scrim = Color.Black,
      surfaceBright = colors.background,
      surfaceDim = colors.neutral,
      surfaceContainerLowest = colors.neutral,
      surfaceContainerLow = colors.neutral,
      surfaceContainer = colors.neutral,
      surfaceContainerHigh = colors.neutral,
      surfaceContainerHighest = colors.neutral,
      error = base.error,
      onError = base.onError,
      errorContainer = base.errorContainer,
      onErrorContainer = base.onErrorContainer,
    ).withExpressiveFixed(dynamic?.ink ?: InkFixed, dynamic?.accent ?: AccentFixed, dynamic?.peach ?: PeachFixed)

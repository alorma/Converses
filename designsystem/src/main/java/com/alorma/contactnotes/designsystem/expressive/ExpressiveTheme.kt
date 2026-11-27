package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.alorma.contactnotes.designsystem.theme.isDarkTheme

/**
 * Tema de l'app expressiva. Va dins de `ConversesTheme`: llegeix si és fosc, hi posa els colors
 * expressius i en deriva l'esquema de Material (sense color dinàmic ni tocar els d'error).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
  val colors = if (isDarkTheme) DarkExpressiveColors else LightExpressiveColors
  val colorScheme =
    MaterialTheme.colorScheme.copy(
      background = colors.background,
      surface = colors.background,
      onBackground = colors.ink,
      onSurface = colors.ink,
      onSurfaceVariant = colors.inkSecondary,
      surfaceVariant = colors.neutral,
      surfaceContainerLowest = colors.neutral,
      surfaceContainerLow = colors.neutral,
      surfaceContainer = colors.neutral,
      surfaceContainerHigh = colors.neutral,
      surfaceContainerHighest = colors.neutral,
      primary = colors.ink,
      onPrimary = colors.background,
      primaryContainer = colors.accent,
      secondaryContainer = colors.accent,
      onPrimaryContainer = colors.onAccent,
      onSecondaryContainer = colors.onAccent,
    )
  CompositionLocalProvider(LocalExpressiveColors provides colors) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = MaterialTheme.typography,
      shapes = MaterialTheme.shapes,
      content = content,
    )
  }
}

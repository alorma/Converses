package com.alorma.contactnotes.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

internal val LocalPersonColorTheme = compositionLocalOf { true }

/** Esquema de l'app, abans que cap [PersonTheme] el sobreescrigui; serveix per tornar-hi des de dins d'una persona. */
internal val LocalAppColorScheme = compositionLocalOf<ColorScheme?> { null }

private const val SchemeAnimationMillis = 300

/**
 * Tema d'una persona: el color de l'avatar és la llavor de l'esquema. Només canvia els colors; la tipografia, les formes
 * i el moviment són els de l'app. Si l'ajust «Color de la persona» és apagat, no fa res.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonTheme(
  color: AvatarColor?,
  content: @Composable () -> Unit,
) {
  if (color == null || !LocalPersonColorTheme.current) {
    content()
    return
  }
  val isDark = isDarkTheme
  val appScheme = LocalAppColorScheme.current ?: MaterialTheme.colorScheme
  val target =
    rememberDynamicColorScheme(
      seedColor = color.seed,
      isDark = isDark,
      style = PaletteStyle.TonalSpot,
    ).withFixedError(isDark)

  CompositionLocalProvider(LocalAppColorScheme provides appScheme) {
    MaterialExpressiveTheme(
      colorScheme = target.animated(),
      motionScheme = MaterialTheme.motionScheme,
      shapes = MaterialTheme.shapes,
      typography = MaterialTheme.typography,
      content = content,
    )
  }
}

/** Torna als colors de l'app dins d'una zona d'una persona (per exemple el full per triar persona). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppColors(content: @Composable () -> Unit) {
  val scheme = LocalAppColorScheme.current
  if (scheme == null) {
    content()
  } else {
    MaterialExpressiveTheme(
      colorScheme = scheme,
      motionScheme = MaterialTheme.motionScheme,
      shapes = MaterialTheme.shapes,
      typography = MaterialTheme.typography,
      content = content,
    )
  }
}

/** Anima cada rol cap a l'esquema actual: en canviar de color a l'editor, el tema es transforma en comptes de saltar. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
  @Composable
  fun Color.animate(): Color = animateColorAsState(this, tween(SchemeAnimationMillis), label = "scheme").value
  return copy(
    primary = primary.animate(),
    onPrimary = onPrimary.animate(),
    primaryContainer = primaryContainer.animate(),
    onPrimaryContainer = onPrimaryContainer.animate(),
    inversePrimary = inversePrimary.animate(),
    secondary = secondary.animate(),
    onSecondary = onSecondary.animate(),
    secondaryContainer = secondaryContainer.animate(),
    onSecondaryContainer = onSecondaryContainer.animate(),
    tertiary = tertiary.animate(),
    onTertiary = onTertiary.animate(),
    tertiaryContainer = tertiaryContainer.animate(),
    onTertiaryContainer = onTertiaryContainer.animate(),
    background = background.animate(),
    onBackground = onBackground.animate(),
    surface = surface.animate(),
    onSurface = onSurface.animate(),
    surfaceVariant = surfaceVariant.animate(),
    onSurfaceVariant = onSurfaceVariant.animate(),
    surfaceTint = surfaceTint.animate(),
    inverseSurface = inverseSurface.animate(),
    inverseOnSurface = inverseOnSurface.animate(),
    outline = outline.animate(),
    outlineVariant = outlineVariant.animate(),
    surfaceBright = surfaceBright.animate(),
    surfaceDim = surfaceDim.animate(),
    surfaceContainer = surfaceContainer.animate(),
    surfaceContainerHigh = surfaceContainerHigh.animate(),
    surfaceContainerHighest = surfaceContainerHighest.animate(),
    surfaceContainerLow = surfaceContainerLow.animate(),
    surfaceContainerLowest = surfaceContainerLowest.animate(),
  )
}

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/** Colors expressius i famílies «fixed» calculats a partir d'una llavor del fons de pantalla. */
data class DynamicExpressive(
  val colors: ExpressiveColors,
  val ink: FixedColors,
  val accent: FixedColors,
  val peach: FixedColors,
)

/**
 * Paleta expressiva amb color dinàmic. Els neutres (fons, tinta, línies) surten de l'esquema [neutral]
 * (`PaletteStyle.Neutral`, el de `ConversesTheme`); l'accent i el préssec, de l'esquema [vibrant], que té color de
 * veritat. La tinta «fixed» és la família secondary dels neutres.
 */
fun dynamicExpressive(
  neutral: ColorScheme,
  vibrant: ColorScheme,
): DynamicExpressive =
  DynamicExpressive(
    colors =
      ExpressiveColors(
        background = neutral.surface,
        ink = neutral.onSurface,
        inkSecondary = neutral.onSurfaceVariant,
        neutral = neutral.surfaceContainerHigh,
        accent = vibrant.primaryContainer,
        onAccent = vibrant.onPrimaryContainer,
        outline = neutral.outline,
        outlineVariant = neutral.outlineVariant,
      ),
    ink =
      FixedColors(
        neutral.secondaryFixed,
        neutral.secondaryFixedDim,
        neutral.onSecondaryFixed,
        neutral.onSecondaryFixedVariant,
      ),
    accent =
      FixedColors(vibrant.primaryFixed, vibrant.primaryFixedDim, vibrant.onPrimaryFixed, vibrant.onPrimaryFixedVariant),
    peach =
      FixedColors(
        vibrant.tertiaryFixed,
        vibrant.tertiaryFixedDim,
        vibrant.onTertiaryFixed,
        vibrant.onTertiaryFixedVariant,
      ),
  )

/**
 * Els dos esquemes de [dynamicExpressive] per a una [seed], sense Compose: el mateix que fa `ExpressiveTheme`.
 * Les famílies «fixed» són iguals en clar i fosc: sempre surten dels esquemes clars.
 */
fun dynamicExpressive(
  seed: Color,
  isDark: Boolean,
): DynamicExpressive {
  val palette = fromSeed(seed, isDark)
  val fixed = if (isDark) fromSeed(seed, isDark = false) else palette
  return palette.copy(ink = fixed.ink, accent = fixed.accent, peach = fixed.peach)
}

private fun fromSeed(
  seed: Color,
  isDark: Boolean,
): DynamicExpressive =
  dynamicExpressive(
    neutral = dynamicColorScheme(seedColor = seed, isDark = isDark, isAmoled = false, style = PaletteStyle.Neutral),
    vibrant = dynamicColorScheme(seedColor = seed, isDark = isDark, isAmoled = false, style = PaletteStyle.TonalSpot),
  )

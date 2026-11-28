package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicExpressiveContrastTest {
  private val seeds =
    mapOf(
      "blue" to Color(0xFF1A73E8),
      "green" to Color(0xFF2E7D32),
      "red" to Color(0xFFD32F2F),
      "yellow" to Color(0xFFFFEB3B),
      "gray" to Color(0xFF757575),
    )

  private fun forEveryPalette(check: (name: String, palette: DynamicExpressive) -> Unit) {
    for ((seedName, seed) in seeds) {
      for (isDark in listOf(false, true)) {
        check("$seedName/${if (isDark) "dark" else "light"}", dynamicExpressive(seed, isDark))
      }
    }
  }

  @Test
  fun textIsReadableOnEverySurface() =
    forEveryPalette { name, p ->
      val c = p.colors
      val pairs =
        mapOf(
          "ink/background" to contrast(c.ink, c.background),
          "ink/neutral" to contrast(c.ink, c.neutral),
          "inkSecondary/background" to contrast(c.inkSecondary, c.background),
          "onAccent/accent" to contrast(c.onAccent, c.accent),
        )
      pairs.forEach { (pair, ratio) -> assertTrue("$pair in $name is $ratio", ratio >= 4.5) }
    }

  @Test
  fun outlineIsVisibleOnTheBackground() =
    forEveryPalette { name, p ->
      val ratio = contrast(p.colors.outline, p.colors.background)
      assertTrue("outline/background in $name is $ratio", ratio >= 3.0)
    }

  @Test
  fun fixedFamiliesAreReadable() =
    forEveryPalette { name, p ->
      mapOf("ink" to p.ink, "accent" to p.accent, "peach" to p.peach).forEach { (family, f) ->
        val onFixed = contrast(f.onFixed, f.fixed)
        val variant = contrast(f.onFixedVariant, f.fixed)
        assertTrue("$family onFixed/fixed in $name is $onFixed", onFixed >= 4.5)
        assertTrue("$family onFixedVariant/fixed in $name is $variant", variant >= 4.5)
      }
    }

  @Test
  fun fixedFamiliesAreTheSameInLightAndDark() {
    for ((seedName, seed) in seeds) {
      val light = dynamicExpressive(seed, isDark = false)
      val dark = dynamicExpressive(seed, isDark = true)
      assertEquals("ink $seedName", light.ink, dark.ink)
      assertEquals("accent $seedName", light.accent, dark.accent)
      assertEquals("peach $seedName", light.peach, dark.peach)
    }
  }

  @Test
  fun neutralPersonPaletteHoldsContrast() =
    forEveryPalette { name, p ->
      val palette = neutralPalette(p.colors, isDark = name.endsWith("dark"))
      val onBackground = contrast(palette.avatar, p.colors.background)
      val inkOnAvatar = contrast(p.colors.ink, palette.avatar)
      assertTrue("avatar/background in $name is $onBackground", onBackground >= 3.0)
      assertTrue("ink/avatar in $name is $inkOnAvatar", inkOnAvatar >= 4.5)
      assertTrue("accent/surface in $name", contrast(palette.accent, palette.surface) >= 4.5)
      assertTrue("onFeatured/featured in $name", contrast(palette.onFeatured, palette.featured) >= 4.5)
    }
}

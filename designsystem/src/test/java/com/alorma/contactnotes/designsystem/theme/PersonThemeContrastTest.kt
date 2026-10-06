package com.alorma.contactnotes.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonThemeContrastTest {

  private fun contrast(a: Color, b: Color): Double {
    val l1 = a.luminance().toDouble()
    val l2 = b.luminance().toDouble()
    return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
  }

  @Test
  fun accentsKeepReadableContrastForEveryPersonColor() {
    for (color in AvatarColor.entries) {
      for (isDark in listOf(false, true)) {
        val scheme = dynamicColorScheme(seedColor = color.seed, isDark = isDark, isAmoled = false, style = PaletteStyle.TonalSpot)
        val pairs = mapOf(
          "onPrimary/primary" to contrast(scheme.onPrimary, scheme.primary),
          "onPrimaryContainer/primaryContainer" to contrast(scheme.onPrimaryContainer, scheme.primaryContainer),
          "onSurface/surface" to contrast(scheme.onSurface, scheme.surface),
          "onSurfaceVariant/surface" to contrast(scheme.onSurfaceVariant, scheme.surface),
        )
        for ((name, ratio) in pairs) {
          assertTrue("$name ${if (isDark) "dark" else "light"} for $color is $ratio", ratio >= 4.5)
        }
      }
    }
  }
}

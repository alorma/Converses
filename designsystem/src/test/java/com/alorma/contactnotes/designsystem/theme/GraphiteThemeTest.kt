package com.alorma.contactnotes.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.dynamicColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphiteThemeTest {
  private fun contrast(
    a: Color,
    b: Color,
  ): Double {
    val l1 = a.luminance().toDouble()
    val l2 = b.luminance().toDouble()
    return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
  }

  private fun scheme(isDark: Boolean) =
    dynamicColorScheme(seedColor = GraphiteSeed, isDark = isDark, isAmoled = false, style = GraphiteStyle)

  @Test
  fun primaryIsBlackInLightAndWhiteInDark() {
    assertEquals(Color.Black, scheme(isDark = false).primary)
    assertEquals(Color.White, scheme(isDark = true).primary)
  }

  @Test
  fun everyRoleIsAPureGrey() {
    for (isDark in listOf(false, true)) {
      val s = scheme(isDark)
      val roles =
        mapOf(
          "primary" to s.primary,
          "onPrimary" to s.onPrimary,
          "primaryContainer" to s.primaryContainer,
          "onPrimaryContainer" to s.onPrimaryContainer,
          "secondary" to s.secondary,
          "secondaryContainer" to s.secondaryContainer,
          "tertiary" to s.tertiary,
          "tertiaryContainer" to s.tertiaryContainer,
          "surface" to s.surface,
          "onSurface" to s.onSurface,
          "onSurfaceVariant" to s.onSurfaceVariant,
          "surfaceContainer" to s.surfaceContainer,
          "surfaceContainerHigh" to s.surfaceContainerHigh,
          "outline" to s.outline,
          "outlineVariant" to s.outlineVariant,
        )
      for ((name, c) in roles) {
        val mode = if (isDark) "dark" else "light"
        assertTrue("$name $mode is tinted: $c", c.red == c.green && c.green == c.blue)
      }
    }
  }

  @Test
  fun textKeepsReadableContrast() {
    for (isDark in listOf(false, true)) {
      val s = scheme(isDark)
      val pairs =
        mapOf(
          "onPrimary/primary" to contrast(s.onPrimary, s.primary),
          "onSurface/surface" to contrast(s.onSurface, s.surface),
          "onSurfaceVariant/surface" to contrast(s.onSurfaceVariant, s.surface),
        )
      for ((name, ratio) in pairs) {
        assertTrue("$name ${if (isDark) "dark" else "light"} is $ratio", ratio >= 4.5)
      }
    }
  }
}

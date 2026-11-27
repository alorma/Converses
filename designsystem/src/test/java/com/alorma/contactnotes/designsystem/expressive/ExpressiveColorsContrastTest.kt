package com.alorma.contactnotes.designsystem.expressive

import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressiveColorsContrastTest {
  @Test
  fun textIsReadableOnEverySurface() {
    for ((mode, colors) in listOf("light" to LightExpressiveColors, "dark" to DarkExpressiveColors)) {
      val pairs =
        mapOf(
          "ink/background" to contrast(colors.ink, colors.background),
          "ink/neutral" to contrast(colors.ink, colors.neutral),
          "inkSecondary/background" to contrast(colors.inkSecondary, colors.background),
          "onAccent/accent" to contrast(colors.onAccent, colors.accent),
        )
      for ((name, ratio) in pairs) {
        assertTrue("$name in $mode mode is $ratio", ratio >= 4.5)
      }
    }
  }
}

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.ui.graphics.Color
import com.alorma.contactnotes.shared.AvatarColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonPaletteContrastTest {
  private val minimum = 4.5

  private fun check(
    name: String,
    palette: PersonPalette,
    colors: ExpressiveColors,
  ) {
    val pairs =
      listOf(
        "accent/surface" to contrast(palette.accent, palette.surface),
        "accent/detailBackground" to contrast(palette.accent, palette.detailBackground),
        "onFeatured/featured" to contrast(palette.onFeatured, palette.featured),
        "ink/surface" to contrast(colors.ink, palette.surface),
      )
    pairs.forEach { (pair, value) ->
      assertTrue("$name $pair = $value", value >= minimum)
    }
  }

  @Test
  fun textIsReadableForEveryPersonColor() {
    listOf(false, true).forEach { dark ->
      val colors = if (dark) DarkExpressiveColors else LightExpressiveColors
      AvatarColor.entries.forEach { check("${it.name} dark=$dark", it.palette(dark), colors) }
      check("neutral dark=$dark", neutralPalette(dark), colors)
    }
  }

  @Test
  fun neutralAvatarIsVisibleAndHoldsInk() {
    listOf(false, true).forEach { dark ->
      val colors = if (dark) DarkExpressiveColors else LightExpressiveColors
      val palette = neutralPalette(dark)
      val onBackground = contrast(palette.avatar, colors.background)
      val inkOnAvatar = contrast(colors.ink, palette.avatar)
      assertTrue("neutral avatar/background dark=$dark = $onBackground", onBackground >= 3.0)
      assertTrue("ink/neutral avatar dark=$dark = $inkOnAvatar", inkOnAvatar >= minimum)
    }
  }

  @Test
  fun givenValuesMatchTheProposal() {
    assertEquals(
      PersonPalette(
        surface = Color(0xFFE2F2E8),
        avatar = Color(0xFFC2E8D0),
        accent = Color(0xFF245C3F),
        detailBackground = Color(0xFFF1F8F3),
        featured = Color(0xFF245C3F),
        onFeatured = Color(0xFFFFFFFF),
      ),
      AvatarColor.Mint.palette(false),
    )
    assertEquals(Color(0xFF1F3328), AvatarColor.Mint.palette(true).surface)
    assertEquals(Color(0xFF10261A), AvatarColor.Mint.palette(true).onFeatured)
  }
}

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.alorma.contactnotes.shared.AvatarColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedColorsTest {
  @Test
  fun everyFixedFamilyIsReadable() {
    val families =
      mapOf("ink" to InkFixed, "accent" to AccentFixed, "peach" to PeachFixed) +
        AvatarColor.entries.associate { it.name to it.primaryFixed() }
    families.forEach { (name, family) ->
      val onFixed = contrast(family.onFixed, family.fixed)
      val onDim = contrast(family.onFixed, family.fixedDim)
      val variant = contrast(family.onFixedVariant, family.fixed)
      assertTrue("$name onFixed/fixed = $onFixed", onFixed >= 4.5)
      assertTrue("$name onFixed/fixedDim = $onDim", onDim >= 4.5)
      assertTrue("$name onFixedVariant/fixed = $variant", variant >= 4.5)
    }
  }

  @Test
  fun personFamilyUsesTheAvatarAndTheDarkText() {
    AvatarColor.entries.forEach {
      val light = it.palette(isDark = false)
      assertEquals("${it.name} fixed", light.avatar, it.primaryFixed().fixed)
      assertEquals("${it.name} onFixedVariant", light.accent, it.primaryFixed().onFixedVariant)
    }
  }

  @Test
  fun givenValuesMatchTheDesign() {
    assertEquals(
      FixedColors(Color(0xFFC2E8D0), Color(0xFFA7D3B8), Color(0xFF002A16), Color(0xFF245C3F)),
      AvatarColor.Mint.primaryFixed(),
    )
    assertEquals(
      FixedColors(Color(0xFFE9E5DA), Color(0xFFD3CEC2), Color(0xFF1C1B17), Color(0xFF524D3E)),
      InkFixed,
    )
    assertEquals(
      FixedColors(Color(0xFFFFD3BA), Color(0xFFECBB9E), Color(0xFF371803), Color(0xFF8A3E22)),
      PeachFixed,
    )
  }

  @Test
  fun fixedRolesAreTheSameInLightAndDark() {
    val light = lightColorScheme().withExpressiveFixed()
    val dark = darkColorScheme().withExpressiveFixed()
    assertEquals(light.primaryFixed, dark.primaryFixed)
    assertEquals(light.secondaryFixedDim, dark.secondaryFixedDim)
    assertEquals(light.onTertiaryFixedVariant, dark.onTertiaryFixedVariant)
    assertEquals(AccentFixed.fixed, light.secondaryFixed)
    assertEquals(PeachFixed.onFixed, dark.onTertiaryFixed)
  }

  @Test
  fun shapeFabIconIsReadableOnItsContainer() {
    val scheme = lightColorScheme().withExpressiveFixed()
    assertTrue(contrast(scheme.tertiaryFixed, scheme.onTertiaryFixedVariant) >= 4.5)
  }

  @Test
  fun personScreensOnlyReplaceThePrimaryFamily() {
    val base = lightColorScheme().withExpressiveFixed()
    val person = base.withPrimaryFixed(AvatarColor.Rose.primaryFixed())
    assertEquals(Color(0xFFF5CADA), person.primaryFixed)
    assertEquals(Color(0xFF8C3456), person.onPrimaryFixedVariant)
    assertEquals(base.secondaryFixed, person.secondaryFixed)
    assertEquals(base.tertiaryFixedDim, person.tertiaryFixedDim)
  }

  @Test
  fun aPersonWithoutColorKeepsTheInkFamily() {
    assertEquals(InkFixed, (null as AvatarColor?).primaryFixed(personColor = true))
    assertEquals(InkFixed, AvatarColor.Mint.primaryFixed(personColor = false))
  }
}

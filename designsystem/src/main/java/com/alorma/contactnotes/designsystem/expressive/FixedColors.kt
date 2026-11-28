package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.alorma.contactnotes.shared.AvatarColor

/**
 * Una família de rols «fixed» de Material (`*Fixed`, `*FixedDim`, `on*Fixed`, `on*FixedVariant`).
 * Són iguals en mode clar i fosc.
 */
data class FixedColors(
  val fixed: Color,
  val fixedDim: Color,
  val onFixed: Color,
  val onFixedVariant: Color,
)

/** Família primary del tema base: la tinta neutra (la barra, «Totes», «Edita»). */
val InkFixed = FixedColors(Color(0xFFE9E5DA), Color(0xFFD3CEC2), Color(0xFF1C1B17), Color(0xFF524D3E))

/** Família secondary: l'accent groc (comptadors, element actiu de la barra). */
val AccentFixed = FixedColors(Color(0xFFF2E8A6), Color(0xFFDDD185), Color(0xFF1C1B17), Color(0xFF584E00))

/** Família tertiary: el préssec (el FAB de nota ràpida). */
val PeachFixed = FixedColors(Color(0xFFFFD3BA), Color(0xFFECBB9E), Color(0xFF371803), Color(0xFF8A3E22))

/**
 * Família primary de les pantalles d'una persona: [FixedColors.fixed] és l'avatar i
 * [FixedColors.onFixedVariant] el seu «text fosc». Sense color, o amb «Color de la persona» apagat, és [ink]
 * (per defecte [InkFixed]; amb color dinàmic, la família de tinta dinàmica).
 */
fun AvatarColor?.primaryFixed(
  personColor: Boolean = true,
  ink: FixedColors = InkFixed,
): FixedColors =
  if (this == null || !personColor) {
    ink
  } else {
    when (this) {
      AvatarColor.Mint -> FixedColors(Color(0xFFC2E8D0), Color(0xFFA7D3B8), Color(0xFF002A16), Color(0xFF245C3F))
      AvatarColor.Lavender -> FixedColors(Color(0xFFD9CFF5), Color(0xFFC3B7E2), Color(0xFF211C3C), Color(0xFF4F3E92))
      AvatarColor.Peach -> FixedColors(Color(0xFFFFD3BA), Color(0xFFECBB9E), Color(0xFF391609), Color(0xFF8A3E22))
      AvatarColor.Sky -> FixedColors(Color(0xFFC6DFF5), Color(0xFFACC9E1), Color(0xFF04233D), Color(0xFF24557F))
      AvatarColor.Rose -> FixedColors(Color(0xFFF5CADA), Color(0xFFE2B2C4), Color(0xFF381321), Color(0xFF8C3456))
      AvatarColor.Lemon -> FixedColors(Color(0xFFEEE39C), Color(0xFFD9CC7A), Color(0xFF282200), Color(0xFF5E5410))
      // Derivada amb la mateixa lògica que les altres.
      AvatarColor.Sand -> FixedColors(Color(0xFFE8DDCB), Color(0xFFD3C7B3), Color(0xFF2A2216), Color(0xFF5E4E32))
    }
  }

/** Substitueix la família primary (pantalles d'una persona); secondary i tertiary es queden. */
fun ColorScheme.withPrimaryFixed(primary: FixedColors): ColorScheme =
  copy(
    primaryFixed = primary.fixed,
    primaryFixedDim = primary.fixedDim,
    onPrimaryFixed = primary.onFixed,
    onPrimaryFixedVariant = primary.onFixedVariant,
  )

/** Posa les tres famílies «fixed» del tema base (per defecte les crema), iguals en clar i fosc. */
fun ColorScheme.withExpressiveFixed(
  ink: FixedColors = InkFixed,
  accent: FixedColors = AccentFixed,
  peach: FixedColors = PeachFixed,
): ColorScheme =
  withPrimaryFixed(ink).copy(
    secondaryFixed = accent.fixed,
    secondaryFixedDim = accent.fixedDim,
    onSecondaryFixed = accent.onFixed,
    onSecondaryFixedVariant = accent.onFixedVariant,
    tertiaryFixed = peach.fixed,
    tertiaryFixedDim = peach.fixedDim,
    onTertiaryFixed = peach.onFixed,
    onTertiaryFixedVariant = peach.onFixedVariant,
  )

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.alorma.contactnotes.designsystem.theme.LocalPersonColorTheme
import com.alorma.contactnotes.designsystem.theme.isDarkTheme
import com.alorma.contactnotes.shared.AvatarColor

/**
 * Colors de les pantalles d'una persona.
 *
 * [accent] és el «text fosc» en clar i el color de l'avatar en fosc: serveix per al nom,
 * les overlines i el botó «Nova entrada». [featured] i [onFeatured] són la targeta destacada.
 */
data class PersonPalette(
  val surface: Color,
  val avatar: Color,
  val accent: Color,
  val detailBackground: Color,
  val featured: Color,
  val onFeatured: Color,
)

private fun light(
  surface: Long,
  avatar: Long,
  accent: Long,
  detailBackground: Long,
) = PersonPalette(
  surface = Color(surface),
  avatar = Color(avatar),
  accent = Color(accent),
  detailBackground = Color(detailBackground),
  featured = Color(accent),
  onFeatured = Color(0xFFFFFFFF),
)

private fun dark(
  surface: Long,
  avatar: Long,
  detailBackground: Long,
  onFeatured: Long,
) = PersonPalette(
  surface = Color(surface),
  avatar = Color(avatar),
  accent = Color(avatar),
  detailBackground = Color(detailBackground),
  featured = Color(avatar),
  onFeatured = Color(onFeatured),
)

/** Paleta d'un color de persona en mode clar o fosc. El `when` és exhaustiu: un color nou no compila sense paleta. */
fun AvatarColor.palette(isDark: Boolean): PersonPalette =
  if (isDark) {
    when (this) {
      AvatarColor.Mint ->
        dark(
          0xFF1F3328,
          0xFFC2E8D0,
          0xFF19231B, // derivat
          0xFF10261A,
        )
      AvatarColor.Lavender ->
        dark(
          0xFF2A2540,
          0xFFD9CFF5,
          0xFF1F1C27, // derivat
          0xFF1E1838, // derivat
        )
      AvatarColor.Peach ->
        dark(
          0xFF3A2A20, // derivat
          0xFFFFD3BA,
          0xFF271E17, // derivat
          0xFF34180A, // derivat
        )
      AvatarColor.Sky ->
        dark(
          0xFF1C2B3A,
          0xFFC6DFF5,
          0xFF181F24, // derivat
          0xFF0F2236, // derivat
        )
      AvatarColor.Rose ->
        dark(
          0xFF3A2129,
          0xFFF5CADA,
          0xFF271A1A, // derivat
          0xFF3A0F22, // derivat
        )
      AvatarColor.Lemon ->
        dark(
          0xFF35311A, // derivat
          0xFFEEE39C,
          0xFF242214, // derivat
          0xFF2A2606, // derivat
        )
      AvatarColor.Sand ->
        dark(
          0xFF332E24, // derivat
          0xFFE8DDCB,
          0xFF232019, // derivat
          0xFF2A2316, // derivat
        )
    }
  } else {
    when (this) {
      AvatarColor.Mint ->
        light(
          0xFFE2F2E8,
          0xFFC2E8D0,
          0xFF245C3F,
          0xFFF1F8F3,
        )
      AvatarColor.Lavender ->
        light(
          0xFFECE7F8,
          0xFFD9CFF5,
          0xFF4F3E92,
          0xFFF6F3FC,
        )
      AvatarColor.Peach ->
        light(
          0xFFFBE7DC,
          0xFFFFD3BA,
          0xFF8A3E22,
          0xFFFDF4EE,
        )
      AvatarColor.Sky ->
        light(
          0xFFE0EDF8,
          0xFFC6DFF5,
          0xFF24557F,
          0xFFF2F7FC,
        )
      AvatarColor.Rose ->
        light(
          0xFFF9E3EA,
          0xFFF5CADA,
          0xFF8C3456,
          0xFFFCF2F5,
        )
      AvatarColor.Lemon ->
        light(
          0xFFF5F0CF,
          0xFFEEE39C,
          0xFF5E5410,
          0xFFFBF9EA,
        )
      AvatarColor.Sand ->
        light(
          0xFFF0E9DC, // derivat
          0xFFE8DDCB,
          0xFF5E4E32, // derivat
          0xFFF9F6F0, // derivat
        )
    }
  }

/**
 * Paleta amb «Color de la persona» apagat: la superfície neutra i la tinta de [colors] (els expressius, fixos o
 * dinàmics). L'avatar és el gris fix amb els colors fixos, i amb els dinàmics surt de la línia (`outline`).
 */
fun neutralPalette(
  colors: ExpressiveColors,
  isDark: Boolean,
): PersonPalette {
  val isStatic = colors == LightExpressiveColors || colors == DarkExpressiveColors
  return PersonPalette(
    surface = colors.neutral,
    avatar =
      when {
        !isStatic -> dynamicNeutralAvatar(colors)
        isDark -> Color(0xFF666153)
        else -> Color(0xFF948E80)
      },
    accent = colors.ink,
    detailBackground = colors.background,
    featured = colors.ink,
    onFeatured = colors.background,
  )
}

/**
 * Avatar neutre amb colors dinàmics: la línia (`outline`) apropada al fons fins que es veu sobre el fons (3:1) i hi
 * aguanta la tinta (4,5:1), com el gris fix.
 */
private fun dynamicNeutralAvatar(colors: ExpressiveColors): Color {
  fun ratio(
    a: Color,
    b: Color,
  ): Float = (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
  return (0..200)
    .map { lerp(colors.outline, colors.background, it / 200f) }
    .firstOrNull { ratio(it, colors.background) >= 3f && ratio(colors.ink, it) >= 4.5f } ?: colors.outline
}

/** Paleta de la persona, o la neutra si no té color o l'usuari ha apagat «Color de la persona». */
@Composable
fun rememberPersonPalette(color: AvatarColor?): PersonPalette {
  val dark = isDarkTheme
  val personColor = LocalPersonColorTheme.current
  val colors = Expressive.colors
  return remember(color, dark, personColor, colors) {
    if (color == null || !personColor) neutralPalette(colors, dark) else color.palette(dark)
  }
}

/**
 * Paleta del color de l'avatar, sempre: no depèn de «Color de la persona» ni de Pro. La fa servir la targeta
 * destacada, que és un element del llistat i no una pantalla de la persona.
 */
@Composable
fun rememberAvatarPalette(color: AvatarColor): PersonPalette {
  val dark = isDarkTheme
  return remember(color, dark) { color.palette(dark) }
}

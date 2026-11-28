package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors de l'app expressiva: crema (o carbó en fosc) amb un únic accent groc. */
data class ExpressiveColors(
  val background: Color,
  val ink: Color,
  val inkSecondary: Color,
  val neutral: Color,
  val accent: Color,
  val onAccent: Color,
  val outline: Color,
  val outlineVariant: Color,
)

val LightExpressiveColors =
  ExpressiveColors(
    background = Color(0xFFFBF8F1),
    ink = Color(0xFF1C1B17),
    inkSecondary = Color(0xFF5A574D),
    neutral = Color(0xFFEFEBE0),
    accent = Color(0xFFF2E8A6),
    onAccent = Color(0xFF1C1B17),
    outline = Color(0xFF7A766B),
    outlineVariant = Color(0xFFD6D1C4),
  )

val DarkExpressiveColors =
  ExpressiveColors(
    background = Color(0xFF14130F),
    ink = Color(0xFFF1EDE2),
    inkSecondary = Color(0xFFB8B2A3),
    neutral = Color(0xFF2A2822),
    accent = Color(0xFFE4D77A),
    onAccent = Color(0xFF1C1B17),
    outline = Color(0xFF948F82),
    outlineVariant = Color(0xFF48453D),
  )

val LocalExpressiveColors = staticCompositionLocalOf { LightExpressiveColors }

object Expressive {
  val colors: ExpressiveColors
    @Composable @ReadOnlyComposable
    get() = LocalExpressiveColors.current
}

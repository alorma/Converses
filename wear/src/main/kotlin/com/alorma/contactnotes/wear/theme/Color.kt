package com.alorma.contactnotes.wear.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

/** Tokens del brief de Wear: el rellotge és sempre fosc i el fons és negre pur (OLED), no el `surface` del mòbil. */
object WearColors {
  val Background = Color(0xFF000000)
  val SurfaceCard = Color(0xFF1C1C1C)
  val SurfaceCardHigh = Color(0xFF252525)
  val PrimaryContainer = Color(0xFF404040)
  val Primary = Color(0xFFE8E8E8)
  val OnPrimary = Color(0xFF161616)
  val OnSurface = Color(0xFFEBEBEB)
  val OnSurfaceVariant = Color(0xFFB7B7B7)
  val Outline = Color(0xFF808080)
  val OutlineVariant = Color(0xFF424242)
  val OnPrimaryContainer = Color(0xFFEBEBEB)
}

/** Pastels d'identitat dels avatars; el text o la icona a sobre sempre va en `AvatarInk`, també al mode fosc. */
enum class AvatarColor(
  val color: Color,
) {
  Lavender(Color(0xFFDCD3F5)),
  Mint(Color(0xFFC9EBD6)),
  Peach(Color(0xFFFFD6BF)),
  Sky(Color(0xFFCBE3F7)),
  Rose(Color(0xFFF7CFDD)),
  Lemon(Color(0xFFF1E9A8)),
  Sand(Color(0xFFE8DDCB)),
}

val AvatarInk = Color(0xFF1E1D24)

internal val ConversesWearColorScheme =
  ColorScheme(
    background = WearColors.Background,
    onBackground = WearColors.OnSurface,
    primary = WearColors.Primary,
    onPrimary = WearColors.OnPrimary,
    primaryContainer = WearColors.PrimaryContainer,
    onPrimaryContainer = WearColors.OnPrimaryContainer,
    surfaceContainerLow = WearColors.SurfaceCard,
    surfaceContainer = WearColors.SurfaceCard,
    surfaceContainerHigh = WearColors.SurfaceCardHigh,
    onSurface = WearColors.OnSurface,
    onSurfaceVariant = WearColors.OnSurfaceVariant,
    outline = WearColors.Outline,
    outlineVariant = WearColors.OutlineVariant,
  )

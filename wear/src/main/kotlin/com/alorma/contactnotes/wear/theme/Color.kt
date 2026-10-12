package com.alorma.contactnotes.wear.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import com.alorma.contactnotes.shared.AvatarColor

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

/** Pastel d'identitat de l'avatar; el text o la icona a sobre sempre va en `AvatarInk`, també al mode fosc. */
val AvatarColor.color: Color get() = Color(argb)

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

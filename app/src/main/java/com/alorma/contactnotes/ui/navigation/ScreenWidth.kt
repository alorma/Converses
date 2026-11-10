package com.alorma.contactnotes.ui.navigation

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * Amplada de la finestra de l'app, que és el que decideix la disposició: mai el model del dispositiu ni l'orientació.
 * Una tauleta en multifinestra o un plegable tancat poden ser [Compact].
 */
enum class ScreenWidth {
  /** Menys de 600 dp: el mòbil. Un panell i la barra flotant. */
  Compact,

  /** De 600 a 839 dp: tauleta vertical, plegable obert. */
  Medium,

  /** 840 dp o més: tauleta horitzontal, escriptori. */
  Expanded,
}

private const val MediumMinWidthDp = 600
private const val ExpandedMinWidthDp = 840

fun screenWidthOf(widthDp: Int): ScreenWidth =
  when {
    widthDp >= ExpandedMinWidthDp -> ScreenWidth.Expanded
    widthDp >= MediumMinWidthDp -> ScreenWidth.Medium
    else -> ScreenWidth.Compact
  }

/** Amplada de la finestra actual; es recalcula en girar, redimensionar, plegar o desplegar. */
@Composable
fun currentScreenWidth(): ScreenWidth = screenWidthOf(currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp)

/** Navegació principal que [AppShell] dibuixa per a la pantalla de dalt de la pila. */
enum class ShellNavigation {
  None,

  /** Barra flotant amb el FAB de nota ràpida: el mòbil, només a les pestanyes. */
  FloatingBar,

  /** Rail a l'esquerra amb la nota ràpida a dalt: pantalles grans. */
  Rail,
}

/**
 * En una pantalla gran el rail acompanya les pestanyes, la persona i Ajustos. No surt a l'onboarding ni a les
 * pantalles modals (nota ràpida, formularis, Pro), on tocar una destinació faria perdre el que s'hi escrivia.
 */
fun shellNavigationFor(
  current: NavKey,
  width: ScreenWidth,
): ShellNavigation =
  when {
    width == ScreenWidth.Compact -> if (current is TopLevelRoute) ShellNavigation.FloatingBar else ShellNavigation.None
    current is TopLevelRoute ||
      current is PersonRoute ||
      current is PersonLockedRoute ||
      current is SettingsRoute ||
      current is CustomChannelsRoute -> ShellNavigation.Rail
    else -> ShellNavigation.None
  }

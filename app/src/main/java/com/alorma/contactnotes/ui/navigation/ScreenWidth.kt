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

/** Ctrl+N (teclat físic): nota ràpida. `KeyEvent.KEYCODE_N` val 42; es passa el codi perquè es pugui provar a la JVM. */
fun isQuickNoteShortcut(
  isDown: Boolean,
  isCtrlPressed: Boolean,
  keyCode: Int,
): Boolean = isDown && isCtrlPressed && keyCode == KeyCodeN

private const val KeyCodeN = 42

/** Nota ràpida, formularis i Pro: al mòbil són pantalles completes; en una pantalla gran, diàlegs sobre la resta. */
fun NavKey.isFormRoute(): Boolean =
  this is QuickNoteRoute ||
    this is EditPersonRoute ||
    this is TaskRoute ||
    this is PaywallRoute ||
    this is SecurityWordsRoute ||
    this is EnterWordsRoute

/**
 * Al mòbil, la barra flotant només a les pestanyes. En una pantalla gran el rail acompanya les pestanyes, la persona
 * i Ajustos, i s'hi queda quan a sobre s'obre un formulari, que és un diàleg i el tapa. No surt a l'onboarding ni
 * quan a la pila només hi ha un formulari (un import des d'una altra app), que ocupa tota la pantalla.
 */
fun shellNavigationFor(
  backStack: List<NavKey>,
  width: ScreenWidth,
  /**
   * Plegable obert amb el llistat i el detall partits per la frontissa: no hi ha rail. La barra flotant va dins del
   * panell del llistat i es veu mentre es veu el llistat, també amb una persona oberta o un diàleg a sobre.
   */
  book: Boolean = false,
): ShellNavigation {
  if (width == ScreenWidth.Compact) {
    return if (backStack.lastOrNull() is TopLevelRoute) ShellNavigation.FloatingBar else ShellNavigation.None
  }
  val screen = backStack.lastOrNull { !it.isFormRoute() }
  if (book) {
    val listVisible = screen is TopLevelRoute || screen is PersonRoute || screen is PersonLockedRoute
    return if (listVisible) ShellNavigation.FloatingBar else ShellNavigation.None
  }
  return if (screen == null || screen is OnboardingRoute) ShellNavigation.None else ShellNavigation.Rail
}

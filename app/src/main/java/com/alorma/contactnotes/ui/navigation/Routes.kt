package com.alorma.contactnotes.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.alorma.contactnotes.ui.edit.EditFocus
import com.alorma.contactnotes.ui.paywall.ProBenefit
import kotlinx.serialization.Serializable

/** Every destination of the app's single back stack. */
sealed interface AppRoute : NavKey

/** Destinations reachable from the floating navigation toolbar (one tab each). */
sealed interface TopLevelRoute : AppRoute

@Serializable
data object OnboardingRoute : AppRoute

@Serializable
data object PeopleRoute : TopLevelRoute

@Serializable
data object ArchivedRoute : TopLevelRoute

/** Secondary screen (no navigation toolbar), opened from an icon, not a tab. */
@Serializable
data object SettingsRoute : AppRoute

@Serializable
data class EditPersonRoute(
  val personId: Long = 0L,
  val returnSelection: Boolean = false,
  val focus: EditFocus? = null,
) : AppRoute

@Serializable
data class QuickNoteRoute(
  val personId: Long = 0L,
  /** Text imported from another app (share or select-text); survives process death. */
  val sharedText: String? = null,
) : AppRoute

@Serializable
data class PersonRoute(
  val id: Long,
  /** Obre el full de nova entrada en arribar (fila «Crear nota» de Contactes); només la primera vegada. */
  val openNewEntry: Boolean = false,
) : AppRoute

@Serializable
data class PersonLockedRoute(
  val id: Long,
  /** Es passa a [PersonRoute] després de desbloquejar. */
  val openNewEntry: Boolean = false,
) : AppRoute

/** Pantalla «Converses Pro»: [highlight] és el benefici d'on ve l'usuari (8b); des d'Ajustos, `null` (8a). */
@Serializable
data class PaywallRoute(
  val highlight: ProBenefit? = null,
) : AppRoute

/** Ajustos › Canals propis (7d): només hi arriba qui té Pro; sense, la fila d'Ajustos porta al paywall. */
@Serializable
data object CustomChannelsRoute : AppRoute

/** Pantalla «Sincronització» (13d, 13i, 13o): el que mostra depèn de l'estat del compte, no de la ruta. */
@Serializable
data object SyncRoute : AppRoute

/**
 * Les paraules de seguretat (13f). Amb [activation], al primer dispositiu: porta el peu de «Les he guardat» i tancar-la
 * tanca la sessió. Sense, és la revisió des de Sincronització. No porta mai les paraules: les dona el repositori.
 */
@Serializable
data class SecurityWordsRoute(
  val activation: Boolean = false,
) : AppRoute

/** Escriure les paraules en un dispositiu nou (13j). */
@Serializable
data object EnterWordsRoute : AppRoute

/** Formulari de tasca (Pro): nova per a [personId] o, amb [taskId], edició. S'hi arriba des del detall. */
@Serializable
data class TaskRoute(
  val personId: Long,
  val taskId: Long = 0L,
) : AppRoute

package com.alorma.contactnotes.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.alorma.contactnotes.ui.edit.EditFocus
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

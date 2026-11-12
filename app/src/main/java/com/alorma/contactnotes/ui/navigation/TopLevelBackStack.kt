package com.alorma.contactnotes.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Single back stack shared by every tab, with the first route ([PeopleRoute], or [OnboardingRoute] on
 * first run) as its base.
 *
 * Tabs do not keep history: going to [ArchivedRoute] leaves `[People, Archived]` and going to
 * [PeopleRoute] leaves `[People]`, so the tab you leave loses its state (scroll, screens opened on top).
 * Secondary routes are pushed on top (`[People, Person]`). Back removes the last entry; with only the
 * base left it belongs to the system, which closes the app with its own animation.
 *
 * @param entries the saveable stack fed to `NavDisplay` (`rememberNavBackStack` in the app). It must
 *   start with the base route.
 */
class TopLevelBackStack(
  private val entries: MutableList<NavKey>,
) {
  init {
    require(entries.isNotEmpty()) { "The back stack needs a base route" }
  }

  /** Entries to feed to `NavDisplay`: base first, current screen last. */
  val backStack: List<NavKey> get() = entries

  /** Last tab of the stack; [PeopleRoute] while only the base (or onboarding) is there. */
  val currentTab: TopLevelRoute
    get() = entries.lastOrNull { it is TopLevelRoute } as? TopLevelRoute ?: PeopleRoute

  val current: NavKey get() = entries.last()

  /** Navigates to a tab, or pushes a secondary route on top. */
  fun navigate(route: AppRoute) {
    when (route) {
      is TopLevelRoute -> switchTab(route)
      else -> entries.add(route)
    }
  }

  /**
   * Id of the person on top of the list (open or still locked), even with other screens above it; `null` when the
   * list has nobody open. On large screens it is the row the list marks as selected.
   */
  val openPersonId: Long?
    get() =
      when (val route = entries.lastOrNull { it.isPerson() }) {
        is PersonRoute -> route.id
        is PersonLockedRoute -> route.id
        else -> null
      }

  /**
   * Opens a person from the list. With two panes the list stays visible while a person is open, so picking another
   * one replaces it instead of stacking: back must return to the list, not to every person visited.
   */
  fun openPerson(route: AppRoute) {
    while (entries.size > 1 && entries.last().isPerson()) entries.removeAt(entries.lastIndex)
    entries.add(route)
  }

  private fun NavKey.isPerson() = this is PersonRoute || this is PersonLockedRoute

  /**
   * Pushes [route] unless it is already in the stack; then it drops what is on top of it. For a destination that
   * stays reachable while it is open (Settings in the rail): pressing it again must not stack a second copy.
   */
  fun navigateSingle(route: AppRoute) {
    val index = entries.indexOf(route)
    if (index < 0) {
      entries.add(route)
    } else {
      while (entries.lastIndex > index) entries.removeAt(entries.lastIndex)
    }
  }

  /** Leaves `[base]` for [PeopleRoute] or `[base, tab]` for any other tab, dropping everything else. */
  fun switchTab(tab: TopLevelRoute) {
    while (entries.size > 1) entries.removeAt(entries.lastIndex)
    if (entries.first() != tab) entries.add(tab)
  }

  /**
   * Pops the current route. Returns `false` when only the base is left, so the system
   * can close the app.
   */
  fun pop(): Boolean {
    if (entries.size <= 1) return false
    entries.removeAt(entries.lastIndex)
    return true
  }

  /**
   * Drops every route about person [id] (detail, locked screen, editor, task, quick note) wherever it is in the stack.
   * For a person that no longer exists: deleted here or by another device through sync. It is idempotent, so the
   * local delete and the detail noticing the person is gone can both call it.
   */
  fun forgetPerson(id: Long) {
    entries.removeAll { route ->
      when (route) {
        is PersonRoute -> route.id == id
        is PersonLockedRoute -> route.id == id
        is EditPersonRoute -> route.personId == id
        is TaskRoute -> route.personId == id
        is QuickNoteRoute -> route.personId == id
        else -> false
      }
    }
    if (entries.isEmpty()) entries.add(PeopleRoute)
  }

  /** Leaves [root] as the only route (onboarding → people). */
  fun resetTo(root: AppRoute = PeopleRoute) {
    entries.add(root)
    while (entries.size > 1) entries.removeAt(0)
  }
}

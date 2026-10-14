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

  /** Leaves [root] as the only route (onboarding → people). */
  fun resetTo(root: AppRoute = PeopleRoute) {
    entries.add(root)
    while (entries.size > 1) entries.removeAt(0)
  }
}

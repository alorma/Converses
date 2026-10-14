package com.alorma.contactnotes.ui.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelBackStackTest {
  private fun stackOf(root: AppRoute = PeopleRoute) = TopLevelBackStack(mutableListOf<NavKey>(root))

  @Test
  fun `starts on people alone`() {
    val stack = stackOf()

    assertEquals(PeopleRoute, stack.currentTab)
    assertEquals(listOf<NavKey>(PeopleRoute), stack.backStack)
  }

  @Test
  fun `switching to archived puts it on top of people`() {
    val stack = stackOf()

    stack.navigate(ArchivedRoute)

    assertEquals(ArchivedRoute, stack.currentTab)
    assertEquals(listOf<NavKey>(PeopleRoute, ArchivedRoute), stack.backStack)
  }

  @Test
  fun `switching to people leaves only people`() {
    val stack = stackOf()
    stack.navigate(ArchivedRoute)
    stack.navigate(PersonRoute(1))

    stack.navigate(PeopleRoute)

    assertEquals(PeopleRoute, stack.currentTab)
    assertEquals(listOf<NavKey>(PeopleRoute), stack.backStack)
  }

  @Test
  fun `tabs do not keep their history`() {
    val stack = stackOf()
    stack.navigate(ArchivedRoute)
    stack.navigate(PersonRoute(1))
    stack.navigate(PeopleRoute)
    stack.navigate(ArchivedRoute)

    assertEquals(listOf<NavKey>(PeopleRoute, ArchivedRoute), stack.backStack)
  }

  @Test
  fun `switching tab from a secondary route drops it`() {
    val stack = stackOf()
    stack.navigate(SettingsRoute)

    stack.switchTab(ArchivedRoute)

    assertEquals(listOf<NavKey>(PeopleRoute, ArchivedRoute), stack.backStack)
  }

  @Test
  fun `secondary routes are pushed on top of the current tab`() {
    val stack = stackOf()
    stack.navigate(ArchivedRoute)
    stack.navigate(PersonRoute(1))

    assertEquals(ArchivedRoute, stack.currentTab)
    assertEquals(listOf<NavKey>(PeopleRoute, ArchivedRoute, PersonRoute(1)), stack.backStack)
  }

  @Test
  fun `settings is a secondary route on the current tab`() {
    val stack = stackOf()
    stack.navigate(SettingsRoute)

    assertEquals(PeopleRoute, stack.currentTab)
    assertEquals(SettingsRoute, stack.current)
    assertTrue(stack.pop())
    assertEquals(PeopleRoute, stack.current)
  }

  @Test
  fun `pop removes the last entry`() {
    val stack = stackOf()
    stack.navigate(PersonRoute(1))

    assertTrue(stack.pop())
    assertEquals(listOf<NavKey>(PeopleRoute), stack.backStack)
  }

  @Test
  fun `pop from a tab goes to people and then belongs to the system`() {
    val stack = stackOf()
    stack.navigate(ArchivedRoute)

    assertTrue(stack.pop())
    assertEquals(PeopleRoute, stack.currentTab)
    assertFalse(stack.pop())
    assertEquals(listOf<NavKey>(PeopleRoute), stack.backStack)
  }

  @Test
  fun `onboarding is the base until reset`() {
    val stack = stackOf(root = OnboardingRoute)

    assertEquals(OnboardingRoute, stack.current)
    assertFalse(stack.pop())

    stack.resetTo()

    assertEquals(listOf<NavKey>(PeopleRoute), stack.backStack)
  }

  @Test
  fun `unlocking a person replaces the locked screen`() {
    val stack = stackOf()
    stack.navigate(PersonLockedRoute(1))

    stack.pop()
    stack.navigate(PersonRoute(1))

    assertEquals(listOf<NavKey>(PeopleRoute, PersonRoute(1)), stack.backStack)
  }
}

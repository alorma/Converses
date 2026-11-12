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
  fun `opening another person replaces the one that was open`() {
    val stack = stackOf()
    stack.openPerson(PersonRoute(1))

    stack.openPerson(PersonRoute(2))

    assertEquals(listOf<NavKey>(PeopleRoute, PersonRoute(2)), stack.backStack)
    assertEquals(2L, stack.openPersonId)
  }

  @Test
  fun `opening a person replaces a locked one and keeps the tab`() {
    val stack = stackOf()
    stack.navigate(ArchivedRoute)
    stack.openPerson(PersonLockedRoute(1))

    stack.openPerson(PersonRoute(2))

    assertEquals(listOf<NavKey>(PeopleRoute, ArchivedRoute, PersonRoute(2)), stack.backStack)
  }

  @Test
  fun `the open person is known with a screen on top and gone after going back`() {
    val stack = stackOf()
    assertEquals(null, stack.openPersonId)

    stack.openPerson(PersonLockedRoute(7))
    stack.navigate(EditPersonRoute(7))
    assertEquals(7L, stack.openPersonId)

    stack.pop()
    stack.pop()
    assertEquals(null, stack.openPersonId)
  }

  @Test
  fun `opening settings twice keeps a single copy`() {
    val stack = stackOf()

    stack.navigateSingle(SettingsRoute)
    stack.navigateSingle(SettingsRoute)

    assertEquals(listOf<NavKey>(PeopleRoute, SettingsRoute), stack.backStack)
  }

  @Test
  fun `opening settings from a screen inside it goes back to settings`() {
    val stack = stackOf()
    stack.navigateSingle(SettingsRoute)
    stack.navigate(CustomChannelsRoute)

    stack.navigateSingle(SettingsRoute)

    assertEquals(listOf<NavKey>(PeopleRoute, SettingsRoute), stack.backStack)
  }

  @Test
  fun `forgetting a person drops every route about them and keeps the rest`() {
    val stack = stackOf()
    stack.navigate(SettingsRoute)
    stack.navigate(PersonRoute(1))
    stack.navigate(EditPersonRoute(personId = 1))
    stack.navigate(TaskRoute(personId = 1))
    stack.navigate(PersonRoute(2))

    stack.forgetPerson(1)

    assertEquals(listOf<NavKey>(PeopleRoute, SettingsRoute, PersonRoute(2)), stack.backStack)
  }

  @Test
  fun `forgetting a person twice changes nothing the second time`() {
    val stack = stackOf()
    stack.navigate(PersonRoute(1))
    stack.navigate(EditPersonRoute(personId = 1))

    stack.forgetPerson(1)
    stack.forgetPerson(1)

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

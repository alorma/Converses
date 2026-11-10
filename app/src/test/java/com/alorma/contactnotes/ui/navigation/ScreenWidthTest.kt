package com.alorma.contactnotes.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenWidthTest {
  @Test
  fun `width classes break at 600 and 840 dp`() {
    assertEquals(ScreenWidth.Compact, screenWidthOf(411))
    assertEquals(ScreenWidth.Compact, screenWidthOf(599))
    assertEquals(ScreenWidth.Medium, screenWidthOf(600))
    assertEquals(ScreenWidth.Medium, screenWidthOf(839))
    assertEquals(ScreenWidth.Expanded, screenWidthOf(840))
    assertEquals(ScreenWidth.Expanded, screenWidthOf(1280))
  }

  @Test
  fun `the phone shows the floating bar only on tabs`() {
    assertEquals(ShellNavigation.FloatingBar, shellNavigationFor(PeopleRoute, ScreenWidth.Compact))
    assertEquals(ShellNavigation.FloatingBar, shellNavigationFor(ArchivedRoute, ScreenWidth.Compact))
    assertEquals(ShellNavigation.None, shellNavigationFor(PersonRoute(1), ScreenWidth.Compact))
    assertEquals(ShellNavigation.None, shellNavigationFor(SettingsRoute, ScreenWidth.Compact))
    assertEquals(ShellNavigation.None, shellNavigationFor(QuickNoteRoute(), ScreenWidth.Compact))
  }

  @Test
  fun `a large screen shows the rail on tabs, people and settings`() {
    for (width in listOf(ScreenWidth.Medium, ScreenWidth.Expanded)) {
      assertEquals(ShellNavigation.Rail, shellNavigationFor(PeopleRoute, width))
      assertEquals(ShellNavigation.Rail, shellNavigationFor(ArchivedRoute, width))
      assertEquals(ShellNavigation.Rail, shellNavigationFor(PersonRoute(1), width))
      assertEquals(ShellNavigation.Rail, shellNavigationFor(PersonLockedRoute(1), width))
      assertEquals(ShellNavigation.Rail, shellNavigationFor(SettingsRoute, width))
      assertEquals(ShellNavigation.Rail, shellNavigationFor(CustomChannelsRoute, width))
    }
  }

  @Test
  fun `a large screen hides the rail on onboarding and on modal screens`() {
    for (width in listOf(ScreenWidth.Medium, ScreenWidth.Expanded)) {
      assertEquals(ShellNavigation.None, shellNavigationFor(OnboardingRoute, width))
      assertEquals(ShellNavigation.None, shellNavigationFor(QuickNoteRoute(), width))
      assertEquals(ShellNavigation.None, shellNavigationFor(EditPersonRoute(), width))
      assertEquals(ShellNavigation.None, shellNavigationFor(TaskRoute(1), width))
      assertEquals(ShellNavigation.None, shellNavigationFor(PaywallRoute(), width))
    }
  }
}

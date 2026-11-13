package com.alorma.contactnotes.ui.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenWidthTest {
  private val large = listOf(ScreenWidth.Medium, ScreenWidth.Expanded)

  private fun shell(
    width: ScreenWidth,
    vararg stack: NavKey,
  ) = shellNavigationFor(stack.toList(), width)

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
    val width = ScreenWidth.Compact
    assertEquals(ShellNavigation.FloatingBar, shell(width, PeopleRoute))
    assertEquals(ShellNavigation.FloatingBar, shell(width, PeopleRoute, ArchivedRoute))
    assertEquals(ShellNavigation.None, shell(width, PeopleRoute, PersonRoute(1)))
    assertEquals(ShellNavigation.None, shell(width, PeopleRoute, SettingsRoute))
    assertEquals(ShellNavigation.None, shell(width, PeopleRoute, QuickNoteRoute()))
  }

  @Test
  fun `a large screen shows the rail on tabs, people and settings`() {
    for (width in large) {
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, ArchivedRoute))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, PersonRoute(1)))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, PersonLockedRoute(1)))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SettingsRoute))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SettingsRoute, CustomChannelsRoute))
    }
  }

  @Test
  fun `a large screen keeps the rail under a form, which is a dialog`() {
    for (width in large) {
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, QuickNoteRoute()))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, PersonRoute(1), EditPersonRoute(1)))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, PersonRoute(1), TaskRoute(1)))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SettingsRoute, PaywallRoute()))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, QuickNoteRoute(), EditPersonRoute()))
    }
  }

  @Test
  fun `sync keeps the rail on a large screen and shows no bar on the phone`() {
    assertEquals(ShellNavigation.None, shell(ScreenWidth.Compact, PeopleRoute, SyncRoute))
    assertEquals(ShellNavigation.None, shell(ScreenWidth.Compact, PeopleRoute, SyncRoute, SecurityWordsRoute()))
    for (width in large) {
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SyncRoute))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SyncRoute, SecurityWordsRoute()))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SyncRoute, EnterWordsRoute))
      assertEquals(ShellNavigation.Rail, shell(width, PeopleRoute, SyncRoute, SlotsRoute))
    }
  }

  @Test
  fun `an open foldable shows the floating bar while the list is visible, never the rail`() {
    fun book(vararg stack: NavKey) = shellNavigationFor(stack.toList(), ScreenWidth.Expanded, book = true)

    assertEquals(ShellNavigation.FloatingBar, book(PeopleRoute))
    assertEquals(ShellNavigation.FloatingBar, book(PeopleRoute, ArchivedRoute))
    assertEquals(ShellNavigation.FloatingBar, book(PeopleRoute, PersonRoute(1)))
    assertEquals(ShellNavigation.FloatingBar, book(PeopleRoute, PersonLockedRoute(1)))
    assertEquals(ShellNavigation.FloatingBar, book(PeopleRoute, PersonRoute(1), QuickNoteRoute(1)))
    assertEquals(ShellNavigation.None, book(PeopleRoute, SettingsRoute))
    assertEquals(ShellNavigation.None, book(OnboardingRoute))
    assertEquals(ShellNavigation.None, book(QuickNoteRoute(sharedText = "hi")))
  }

  @Test
  fun `only ctrl plus n pressed down is the quick note shortcut`() {
    val n = 42
    assertEquals(true, isQuickNoteShortcut(isDown = true, isCtrlPressed = true, keyCode = n))
    assertEquals(false, isQuickNoteShortcut(isDown = false, isCtrlPressed = true, keyCode = n))
    assertEquals(false, isQuickNoteShortcut(isDown = true, isCtrlPressed = false, keyCode = n))
    assertEquals(false, isQuickNoteShortcut(isDown = true, isCtrlPressed = true, keyCode = n + 1))
  }

  @Test
  fun `a large screen hides the rail on onboarding and on an import`() {
    for (width in large) {
      assertEquals(ShellNavigation.None, shell(width, OnboardingRoute))
      assertEquals(ShellNavigation.None, shell(width, QuickNoteRoute(sharedText = "hi")))
      assertEquals(ShellNavigation.None, shell(width, QuickNoteRoute(sharedText = "hi"), EditPersonRoute()))
    }
  }
}

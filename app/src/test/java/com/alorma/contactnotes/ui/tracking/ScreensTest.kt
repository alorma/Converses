package com.alorma.contactnotes.ui.tracking

import com.alorma.contactnotes.ui.navigation.AppRoute
import com.alorma.contactnotes.ui.navigation.ArchivedRoute
import com.alorma.contactnotes.ui.navigation.CustomChannelsRoute
import com.alorma.contactnotes.ui.navigation.EditPersonRoute
import com.alorma.contactnotes.ui.navigation.OnboardingRoute
import com.alorma.contactnotes.ui.navigation.PaywallRoute
import com.alorma.contactnotes.ui.navigation.PeopleRoute
import com.alorma.contactnotes.ui.navigation.PersonLockedRoute
import com.alorma.contactnotes.ui.navigation.PersonRoute
import com.alorma.contactnotes.ui.navigation.QuickNoteRoute
import com.alorma.contactnotes.ui.navigation.SettingsRoute
import com.alorma.contactnotes.ui.navigation.TaskRoute
import com.alorma.contactnotes.ui.paywall.ProBenefit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ScreensTest {
  private fun AppRoute.tracked(): Pair<String, Map<String, String>>? =
    toScreen()?.let { it.eventName to it.eventProperties }

  @Test
  fun simpleScreens() {
    assertEquals("onboarding" to emptyMap<String, String>(), OnboardingRoute.tracked())
    assertEquals("people" to emptyMap<String, String>(), PeopleRoute.tracked())
    assertEquals("archived" to emptyMap<String, String>(), ArchivedRoute.tracked())
    assertEquals("settings" to emptyMap<String, String>(), SettingsRoute.tracked())
    assertEquals("custom_channels" to emptyMap<String, String>(), CustomChannelsRoute.tracked())
  }

  @Test
  fun personCarriesNoId() {
    assertEquals("person" to emptyMap<String, String>(), PersonRoute(42L, openNewEntry = true).tracked())
  }

  @Test
  fun lockedPersonIsNotTracked() {
    assertNull(PersonLockedRoute(42L).toScreen())
  }

  @Test
  fun editPersonMode() {
    assertEquals(mapOf("mode" to "create"), EditPersonRoute().tracked()?.second)
    assertEquals(mapOf("mode" to "edit"), EditPersonRoute(personId = 42L).tracked()?.second)
  }

  @Test
  fun quickNoteNeverCarriesTheSharedText() {
    assertEquals(
      "quick_note" to mapOf("with_person" to "false", "source" to "app"),
      QuickNoteRoute().tracked(),
    )
    val shared = QuickNoteRoute(personId = 42L, sharedText = "secret text").tracked()
    assertEquals(mapOf("with_person" to "true", "source" to "share"), shared?.second)
  }

  @Test
  fun taskMode() {
    assertEquals("task" to mapOf("mode" to "create"), TaskRoute(personId = 42L).tracked())
    assertEquals(mapOf("mode" to "edit"), TaskRoute(personId = 42L, taskId = 7L).tracked()?.second)
  }

  @Test
  fun paywallHighlight() {
    assertEquals("paywall" to mapOf("highlight" to "none"), PaywallRoute().tracked())
    assertEquals(mapOf("highlight" to "followups"), PaywallRoute(ProBenefit.FollowUps).tracked()?.second)
    assertEquals(mapOf("highlight" to "sync"), PaywallRoute(ProBenefit.Devices).tracked()?.second)
  }

  @Test
  fun noPropertyCarriesIdsOrText() {
    val routes =
      listOf(
        PersonRoute(42L),
        EditPersonRoute(personId = 42L),
        QuickNoteRoute(personId = 42L, sharedText = "secret text"),
        TaskRoute(personId = 42L, taskId = 7L),
      )
    routes.forEach { route ->
      route.toScreen()!!.eventProperties.values.forEach { value ->
        assertFalse("$route leaks $value", value.contains("42") || value.contains("7") || value.contains("secret"))
      }
    }
  }
}

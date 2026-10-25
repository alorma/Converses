package com.alorma.contactnotes.ui.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
  @Test
  fun aPersonRouteSavedBeforeOpenNewEntryStillLoads() {
    assertEquals(PersonRoute(3), Json.decodeFromString(PersonRoute.serializer(), """{"id":3}"""))
    assertEquals(PersonLockedRoute(3), Json.decodeFromString(PersonLockedRoute.serializer(), """{"id":3}"""))
  }

  @Test
  fun openNewEntrySurvivesARoundTrip() {
    val route = PersonLockedRoute(3, openNewEntry = true)
    assertEquals(route, Json.decodeFromString(PersonLockedRoute.serializer(), Json.encodeToString(route)))
  }
}

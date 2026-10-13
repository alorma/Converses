package com.alorma.contactnotes.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionInstallTest {
  private val near = CompanionNode(id = "a", displayName = "Watch A", isNearby = true)
  private val far = CompanionNode(id = "b", displayName = "Watch B", isNearby = false)

  @Test
  fun nodeWithCapabilityMeansPresent() {
    assertEquals(CompanionState.Present, companionStateOf(withApp = listOf(far), connected = listOf(near)))
  }

  @Test
  fun connectedNodeWithoutCapabilityMeansMissing() {
    assertEquals(CompanionState.Missing(near), companionStateOf(withApp = emptyList(), connected = listOf(near)))
  }

  @Test
  fun prefersTheNearbyNode() {
    assertEquals(CompanionState.Missing(near), companionStateOf(emptyList(), listOf(far, near)))
  }

  @Test
  fun fallsBackToNodeNotNearby() {
    assertEquals(CompanionState.Missing(far), companionStateOf(emptyList(), listOf(far)))
  }

  @Test
  fun noNodesMeansNoDevice() {
    assertEquals(CompanionState.NoDevice, companionStateOf(emptyList(), emptyList()))
  }

  @Test
  fun playStoreUriTargetsApplicationId() {
    assertEquals("market://details?id=com.example.app", playStoreUri("com.example.app"))
  }

  @Test
  fun playStoreUriDropsTheDebugSuffix() {
    assertEquals("market://details?id=com.example.app", playStoreUri("com.example.app.dev"))
  }
}

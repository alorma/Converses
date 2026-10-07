package com.alorma.contactnotes.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateTest {
  @Test
  fun proIsOffByDefaultAndCanBeToggledForDebug() {
    val gate = FeatureGate()
    assertFalse(gate.proEnabled.value)

    gate.setProEnabled(true)
    assertTrue(gate.proEnabled.value)

    gate.setProEnabled(false)
    assertFalse(gate.proEnabled.value)
  }
}

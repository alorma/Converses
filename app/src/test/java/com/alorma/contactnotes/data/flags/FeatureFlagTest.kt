package com.alorma.contactnotes.data.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ara mateix l'app no té cap flag: els tests fan servir els seus. */
class FeatureFlagTest {
  private class TestFlag(
    runner: FeatureFlagRunner,
    key: String,
    defaultValue: Boolean = false,
    pro: Boolean = false,
  ) : FeatureFlag(runner, key, defaultValue, pro)

  private val runner = LocalFeatureFlagRunner()
  private val finished = TestFlag(runner, key = "finished", defaultValue = true)
  private val unfinished = TestFlag(runner, key = "unfinished")
  private val proA = TestFlag(runner, key = "pro_a", pro = true)
  private val proB = TestFlag(runner, key = "pro_b", pro = true)

  @Test
  fun aFlagStartsAtItsDefault() {
    assertTrue(finished.isEnabled())
    assertFalse(unfinished.isEnabled())
  }

  @Test
  fun eachFlagIsOverriddenIndependently() {
    runner.setOverride(finished.key, false)
    runner.setOverride(unfinished.key, true)
    assertFalse(finished.isEnabled())
    assertTrue(unfinished.isEnabled())

    runner.clearOverride(finished.key)
    assertTrue(finished.isEnabled())
    assertTrue(unfinished.isEnabled())

    runner.clearAllOverrides()
    assertFalse(unfinished.isEnabled())
  }

  @Test
  fun changesEmitWhenAnOverrideChanges() {
    val before = runner.changes.value
    runner.setOverride(finished.key, false)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun overridesArePersistedAndRestored() {
    var saved: Map<String, Boolean> = emptyMap()
    val first = LocalFeatureFlagRunner(persist = { saved = it })
    first.setOverride(finished.key, false)
    first.setOverride(unfinished.key, true)
    first.clearOverride(unfinished.key)

    val restored = LocalFeatureFlagRunner(initial = saved)
    assertFalse(TestFlag(restored, key = finished.key, defaultValue = true).isEnabled())
    assertFalse(restored.hasOverride(unfinished.key))
  }

  @Test
  fun proModeSwitchesEveryProFlagAtOnce() {
    val flags = listOf(finished, unfinished, proA, proB)

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, true) }
    assertTrue(proA.isEnabled() && proB.isEnabled())

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, false) }
    assertFalse(proA.isEnabled() || proB.isEnabled())
    assertTrue(finished.isEnabled())
  }
}

package com.alorma.contactnotes.data.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagTest {
  private val runner = LocalFeatureFlagRunner()
  private val avatar = CustomAvatarFeature(runner)
  private val personColor = PersonColorFeature(runner)
  private val channels = CustomChannelsFeature(runner)
  private val followUps = FollowUpsFeature(runner)
  private val all = listOf(avatar, personColor, channels, followUps)

  @Test
  fun everyFinishedFeatureIsOnByDefault() {
    all.forEach { assertTrue(it.key, it.isEnabled()) }
  }

  @Test
  fun keysAreUnique() {
    val keys = all.map { it.key }
    assertTrue(keys.size == keys.toSet().size)
  }

  @Test
  fun eachFlagIsOverriddenIndependently() {
    runner.setOverride(avatar.key, false)
    runner.setOverride(personColor.key, false)
    assertFalse(avatar.isEnabled())
    assertFalse(personColor.isEnabled())

    runner.clearOverride(avatar.key)
    assertTrue(avatar.isEnabled())
    assertFalse(personColor.isEnabled())

    runner.clearAllOverrides()
    assertTrue(personColor.isEnabled())
  }

  @Test
  fun changesEmitWhenAnOverrideChanges() {
    val before = runner.changes.value
    runner.setOverride(personColor.key, false)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun overridesArePersistedAndRestored() {
    var saved: Map<String, Boolean> = emptyMap()
    val first = LocalFeatureFlagRunner(persist = { saved = it })
    first.setOverride(avatar.key, false)
    first.setOverride(personColor.key, false)
    first.clearOverride(personColor.key)

    val restored = LocalFeatureFlagRunner(initial = saved)
    assertFalse(CustomAvatarFeature(restored).isEnabled())
    assertFalse(restored.hasOverride(personColor.key))
  }

  @Test
  fun proModeSwitchesEveryProFlagAtOnce() {
    val flags = all
    assertTrue(flags.all { it.pro })

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, true) }
    assertTrue(flags.all { it.isEnabled() })

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, false) }
    assertTrue(flags.none { it.isEnabled() })
  }
}

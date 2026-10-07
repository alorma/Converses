package com.alorma.contactnotes.data.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagTest {
  private val runner = LocalFeatureFlagRunner()
  private val avatar = CustomAvatarFeature(runner)
  private val channels = CustomChannelsFeature(runner)
  private val followUps = FollowUpsFeature(runner)
  private val contacts = ContactIntegrationFeature(runner)

  @Test
  fun everythingIsOffByDefault() {
    listOf(avatar, channels, followUps, contacts).forEach { assertFalse(it.key, it.isEnabled()) }
  }

  @Test
  fun keysAreUnique() {
    val keys = listOf(avatar, channels, followUps, contacts).map { it.key }
    assertTrue(keys.size == keys.toSet().size)
  }

  @Test
  fun eachFlagIsOverriddenIndependently() {
    runner.setOverride(avatar.key, true)
    runner.setOverride(followUps.key, true)
    assertTrue(avatar.isEnabled())
    assertTrue(followUps.isEnabled())
    assertFalse(channels.isEnabled())
    assertFalse(contacts.isEnabled())

    runner.clearOverride(avatar.key)
    assertFalse(avatar.isEnabled())
    assertTrue(followUps.isEnabled())

    runner.clearAllOverrides()
    assertFalse(followUps.isEnabled())
  }

  @Test
  fun changesEmitWhenAnOverrideChanges() {
    val before = runner.changes.value
    runner.setOverride(channels.key, true)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun overridesArePersistedAndRestored() {
    var saved: Map<String, Boolean> = emptyMap()
    val first = LocalFeatureFlagRunner(persist = { saved = it })
    first.setOverride(avatar.key, true)
    first.setOverride(channels.key, false)
    first.clearOverride(channels.key)

    val restored = LocalFeatureFlagRunner(initial = saved)
    assertTrue(CustomAvatarFeature(restored).isEnabled())
    assertFalse(restored.hasOverride(channels.key))
  }

  @Test
  fun proModeSwitchesEveryProFlagAtOnce() {
    val flags = listOf(avatar, channels, followUps, contacts)
    assertTrue(flags.all { it.pro })

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, true) }
    assertTrue(flags.all { it.isEnabled() })

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, false) }
    assertTrue(flags.none { it.isEnabled() })
  }
}

package com.alorma.contactnotes.data.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagTest {
  private val runner = LocalFeatureFlagRunner()
  private val avatar = CustomAvatarFeature(runner)
  private val personColor = PersonColorFeature(runner)
  private val contactLink = ContactLinkFeature(runner)

  @Test
  fun everythingIsOffByDefault() {
    listOf(avatar, personColor, contactLink).forEach { assertFalse(it.key, it.isEnabled()) }
  }

  @Test
  fun keysAreUnique() {
    val keys = listOf(avatar, personColor, contactLink).map { it.key }
    assertTrue(keys.size == keys.toSet().size)
  }

  @Test
  fun eachFlagIsOverriddenIndependently() {
    runner.setOverride(avatar.key, true)
    runner.setOverride(personColor.key, true)
    assertTrue(avatar.isEnabled())
    assertTrue(personColor.isEnabled())

    runner.clearOverride(avatar.key)
    assertFalse(avatar.isEnabled())
    assertTrue(personColor.isEnabled())

    runner.clearAllOverrides()
    assertFalse(personColor.isEnabled())
  }

  @Test
  fun changesEmitWhenAnOverrideChanges() {
    val before = runner.changes.value
    runner.setOverride(personColor.key, true)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun overridesArePersistedAndRestored() {
    var saved: Map<String, Boolean> = emptyMap()
    val first = LocalFeatureFlagRunner(persist = { saved = it })
    first.setOverride(avatar.key, true)
    first.setOverride(personColor.key, false)
    first.clearOverride(personColor.key)

    val restored = LocalFeatureFlagRunner(initial = saved)
    assertTrue(CustomAvatarFeature(restored).isEnabled())
    assertFalse(restored.hasOverride(personColor.key))
  }

  @Test
  fun proModeSwitchesEveryProFlagAtOnce() {
    val flags = listOf(avatar, personColor, contactLink)
    assertTrue(flags.all { it.pro })

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, true) }
    assertTrue(flags.all { it.isEnabled() })

    flags.filter { it.pro }.forEach { runner.setOverride(it.key, false) }
    assertTrue(flags.none { it.isEnabled() })
  }
}

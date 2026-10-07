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
}

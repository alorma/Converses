package com.alorma.contactnotes.data.flags

import com.alorma.contactnotes.data.pro.LocalProDataSource
import com.alorma.contactnotes.data.pro.ProRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProFeatureFlagRunnerTest {
  private val local = LocalFeatureFlagRunner()
  private val pro = LocalProDataSource()
  private val runner = ProFeatureFlagRunner(local, ProRepository(pro), CoroutineScope(UnconfinedTestDispatcher()))
  private val contactLink = ContactLinkFeature(runner)
  private val free = object : FeatureFlag(runner, key = "free_feature") {}

  @Test
  fun flagOffIsHiddenWithoutPro() {
    assertEquals(FeatureState.Hidden, contactLink.state())
    assertFalse(contactLink.isEnabled())
  }

  @Test
  fun flagOffIsHiddenWithPro() {
    pro.setPro(true)
    assertEquals(FeatureState.Hidden, contactLink.state())
    assertFalse(contactLink.isEnabled())
  }

  @Test
  fun flagOnIsLockedWithoutPro() {
    local.setOverride(contactLink.key, true)
    assertEquals(FeatureState.Locked, contactLink.state())
    assertFalse(contactLink.isEnabled())
  }

  @Test
  fun flagOnIsUnlockedWithPro() {
    local.setOverride(contactLink.key, true)
    pro.setPro(true)
    assertEquals(FeatureState.Unlocked, contactLink.state())
    assertTrue(contactLink.isEnabled())
  }

  @Test
  fun flagsThatAreNotProIgnorePro() {
    assertEquals(FeatureState.Hidden, free.state())
    local.setOverride(free.key, true)
    assertEquals(FeatureState.Unlocked, free.state())
  }

  @Test
  fun losingProLocksAgain() {
    local.setOverride(contactLink.key, true)
    pro.setPro(true)
    pro.setPro(false)
    assertEquals(FeatureState.Locked, contactLink.state())
  }

  @Test
  fun changesEmitWhenTheFlagChanges() {
    val before = runner.changes.value
    local.setOverride(contactLink.key, true)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun changesEmitWhenProChanges() {
    val before = runner.changes.value
    pro.setPro(true)
    assertNotEquals(before, runner.changes.value)
  }
}

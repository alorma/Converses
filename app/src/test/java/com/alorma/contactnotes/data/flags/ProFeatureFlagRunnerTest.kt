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
  private val proFeature = object : FeatureFlag(runner, key = "pro_feature", defaultValue = true, pro = true) {}
  private val free = object : FeatureFlag(runner, key = "free_feature") {}

  @Test
  fun flagOffIsHiddenWithoutPro() {
    local.setOverride(proFeature.key, false)
    assertEquals(FeatureState.Hidden, proFeature.state())
    assertFalse(proFeature.isEnabled())
  }

  @Test
  fun flagOffIsHiddenWithPro() {
    local.setOverride(proFeature.key, false)
    pro.setPro(true)
    assertEquals(FeatureState.Hidden, proFeature.state())
    assertFalse(proFeature.isEnabled())
  }

  @Test
  fun flagOnIsLockedWithoutPro() {
    local.setOverride(proFeature.key, true)
    assertEquals(FeatureState.Locked, proFeature.state())
    assertFalse(proFeature.isEnabled())
  }

  @Test
  fun flagOnIsUnlockedWithPro() {
    local.setOverride(proFeature.key, true)
    pro.setPro(true)
    assertEquals(FeatureState.Unlocked, proFeature.state())
    assertTrue(proFeature.isEnabled())
  }

  @Test
  fun flagsThatAreNotProIgnorePro() {
    assertEquals(FeatureState.Hidden, free.state())
    local.setOverride(free.key, true)
    assertEquals(FeatureState.Unlocked, free.state())
  }

  @Test
  fun losingProLocksAgain() {
    local.setOverride(proFeature.key, true)
    pro.setPro(true)
    pro.setPro(false)
    assertEquals(FeatureState.Locked, proFeature.state())
  }

  @Test
  fun changesEmitWhenTheFlagChanges() {
    val before = runner.changes.value
    local.setOverride(proFeature.key, false)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun aFinishedProFeatureIsLockedByDefaultUnlockedWithProAndHiddenWhenTurnedOff() {
    assertEquals(FeatureState.Locked, proFeature.state())
    pro.setPro(true)
    assertEquals(FeatureState.Unlocked, proFeature.state())
    local.setOverride(proFeature.key, false)
    assertEquals(FeatureState.Hidden, proFeature.state())
  }

  @Test
  fun changesEmitWhenProChanges() {
    val before = runner.changes.value
    pro.setPro(true)
    assertNotEquals(before, runner.changes.value)
  }
}

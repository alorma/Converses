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
  private val personColor = PersonColorFeature(runner)
  private val free = object : FeatureFlag(runner, key = "free_feature") {}

  @Test
  fun flagOffIsHiddenWithoutPro() {
    assertEquals(FeatureState.Hidden, personColor.state())
    assertFalse(personColor.isEnabled())
  }

  @Test
  fun flagOffIsHiddenWithPro() {
    pro.setPro(true)
    assertEquals(FeatureState.Hidden, personColor.state())
    assertFalse(personColor.isEnabled())
  }

  @Test
  fun flagOnIsLockedWithoutPro() {
    local.setOverride(personColor.key, true)
    assertEquals(FeatureState.Locked, personColor.state())
    assertFalse(personColor.isEnabled())
  }

  @Test
  fun flagOnIsUnlockedWithPro() {
    local.setOverride(personColor.key, true)
    pro.setPro(true)
    assertEquals(FeatureState.Unlocked, personColor.state())
    assertTrue(personColor.isEnabled())
  }

  @Test
  fun flagsThatAreNotProIgnorePro() {
    assertEquals(FeatureState.Hidden, free.state())
    local.setOverride(free.key, true)
    assertEquals(FeatureState.Unlocked, free.state())
  }

  @Test
  fun losingProLocksAgain() {
    local.setOverride(personColor.key, true)
    pro.setPro(true)
    pro.setPro(false)
    assertEquals(FeatureState.Locked, personColor.state())
  }

  @Test
  fun changesEmitWhenTheFlagChanges() {
    val before = runner.changes.value
    local.setOverride(personColor.key, true)
    assertNotEquals(before, runner.changes.value)
  }

  @Test
  fun changesEmitWhenProChanges() {
    val before = runner.changes.value
    pro.setPro(true)
    assertNotEquals(before, runner.changes.value)
  }
}

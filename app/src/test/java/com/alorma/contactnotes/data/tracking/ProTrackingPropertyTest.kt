package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.data.pro.LocalProDataSource
import com.alorma.contactnotes.data.pro.ProRepository
import com.alorma.contactnotes.testing.RecordingTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProTrackingPropertyTest {
  private class TestAction : Action("test_action", 1)

  private val source = LocalProDataSource(initial = true)
  private val recorder = RecordingTracker()
  private val scope = TestScope(UnconfinedTestDispatcher())

  // No executa res fins que algú l'avança: comprova que el valor inicial no depèn del collect
  private val idleScope = TestScope()

  private fun lastIsPro(): String? {
    recorder.eventTracker.trackAction(TestAction())
    return recorder.actions.last().eventProperties["is_pro"]
  }

  @Test
  fun initialValueIsSetSynchronously() {
    ProTrackingProperty(ProRepository(source), recorder.eventTracker, idleScope).start()

    assertEquals("true", lastIsPro())
  }

  @Test
  fun followsProChanges() {
    ProTrackingProperty(ProRepository(source), recorder.eventTracker, scope).start()

    source.setPro(false)
    assertEquals("false", lastIsPro())

    source.setPro(true)
    assertEquals("true", lastIsPro())
  }
}

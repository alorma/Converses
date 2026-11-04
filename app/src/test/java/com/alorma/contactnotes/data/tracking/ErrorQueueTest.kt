package com.alorma.contactnotes.data.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorQueueTest {
  private class ErrorRecorder : Tracker {
    override val isRemote: Boolean = false

    val errors = mutableListOf<String>()

    override fun trackScreen(screen: Screen) = Unit

    override fun trackEvent(event: Event) = Unit

    override fun trackAction(action: Action) = Unit

    override fun trackError(
      error: Throwable,
      where: String,
      properties: Map<String, String>,
    ) {
      errors += where
    }
  }

  private val queue = ErrorQueue()
  private val tracker = ErrorRecorder()

  @Test
  fun errorsBeforeAttachArriveInOrderOnAttach() {
    queue.report(IllegalStateException(), ErrorSites.DATASTORE_SETTINGS)
    queue.report(IllegalStateException(), ErrorSites.DATASTORE_PRO)
    assertEquals(emptyList<String>(), tracker.errors)

    queue.attach(tracker)

    assertEquals(listOf(ErrorSites.DATASTORE_SETTINGS, ErrorSites.DATASTORE_PRO), tracker.errors)
  }

  @Test
  fun errorsAfterAttachGoStraightThrough() {
    queue.attach(tracker)

    queue.report(IllegalStateException(), ErrorSites.DATASTORE_FLAGS)

    assertEquals(listOf(ErrorSites.DATASTORE_FLAGS), tracker.errors)
  }

  @Test
  fun queuedErrorsAreSentOnlyOnce() {
    queue.report(IllegalStateException(), ErrorSites.DATASTORE_SETTINGS)
    queue.attach(tracker)
    queue.attach(tracker)

    assertEquals(listOf(ErrorSites.DATASTORE_SETTINGS), tracker.errors)
  }
}

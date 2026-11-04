package com.alorma.contactnotes.data.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTrackerTest {
  private class TestScreen : Screen("test_screen", 1)

  private class TestEvent : Event("test_event", 2)

  private class TestAction(
    source: String,
  ) : Action("test_action", 1, mapOf("source" to source))

  private class RecordingTracker(
    override val isRemote: Boolean,
  ) : Tracker {
    val calls = mutableListOf<String>()

    override fun trackScreen(screen: Screen) {
      calls += "screen:${screen.eventName}"
    }

    override fun trackEvent(event: Event) {
      calls += "event:${event.eventName}"
    }

    override fun trackAction(action: Action) {
      calls += "action:${action.eventName}"
    }

    override fun trackError(
      error: Throwable,
      where: String,
    ) {
      calls += "error:$where"
    }
  }

  private val local = RecordingTracker(isRemote = false)
  private val remote = RecordingTracker(isRemote = true)
  private var consent = false
  private val tracker = EventTracker(listOf(local, remote)) { consent }

  private fun trackAll() {
    tracker.trackScreen(TestScreen())
    tracker.trackEvent(TestEvent())
    tracker.trackAction(TestAction("fab"))
    tracker.trackError(IllegalStateException("secret"), "export_person")
  }

  private val all = listOf("screen:test_screen", "event:test_event", "action:test_action", "error:export_person")

  @Test
  fun withoutConsentOnlyLocalTrackersReceive() {
    consent = false

    trackAll()

    assertEquals(all, local.calls)
    assertTrue(remote.calls.isEmpty())
  }

  @Test
  fun withConsentAllTrackersReceive() {
    consent = true

    trackAll()

    assertEquals(all, local.calls)
    assertEquals(all, remote.calls)
  }

  @Test
  fun consentIsCheckedOnEveryCall() {
    consent = true
    tracker.trackScreen(TestScreen())
    consent = false
    tracker.trackScreen(TestScreen())

    assertEquals(listOf("screen:test_screen"), remote.calls)
    assertEquals(listOf("screen:test_screen", "screen:test_screen"), local.calls)
  }

  @Test
  fun subclassesCarryNameVersionAndProperties() {
    val action = TestAction("fab")

    assertEquals("test_action", action.eventName)
    assertEquals(1, action.eventVersion)
    assertEquals(mapOf("source" to "fab"), action.eventProperties)
    assertEquals(2, TestEvent().eventVersion)
  }
}

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
    val properties = mutableListOf<Map<String, String>>()
    val versions = mutableListOf<Int>()

    override fun trackScreen(screen: Screen) {
      calls += "screen:${screen.eventName}"
      properties += screen.eventProperties
      versions += screen.eventVersion
    }

    override fun trackEvent(event: Event) {
      calls += "event:${event.eventName}"
      properties += event.eventProperties
      versions += event.eventVersion
    }

    override fun trackAction(action: Action) {
      calls += "action:${action.eventName}"
      properties += action.eventProperties
      versions += action.eventVersion
    }

    override fun trackError(
      error: Throwable,
      where: String,
      properties: Map<String, String>,
    ) {
      calls += "error:$where"
      this.properties += properties
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

  @Test
  fun withoutCommonPropertiesEventsAreUntouched() {
    trackAll()

    assertEquals(listOf(emptyMap(), emptyMap(), mapOf("source" to "fab"), emptyMap()), local.properties)
  }

  @Test
  fun commonPropertiesReachEveryCallAndKeepNameAndVersion() {
    consent = true
    tracker.setCommonProperty("is_pro", "true")

    trackAll()

    val expected =
      listOf(
        mapOf("is_pro" to "true"),
        mapOf("is_pro" to "true"),
        mapOf("is_pro" to "true", "source" to "fab"),
        mapOf("is_pro" to "true"),
      )
    assertEquals(expected, local.properties)
    assertEquals(expected, remote.properties)
    assertEquals(all, local.calls)
    assertEquals(listOf(1, 2, 1), local.versions)
  }

  @Test
  fun commonPropertiesAreReadOnEveryCall() {
    tracker.trackScreen(TestScreen())
    tracker.setCommonProperty("is_pro", "false")
    tracker.trackScreen(TestScreen())
    tracker.setCommonProperty("is_pro", "true")
    tracker.trackScreen(TestScreen())
    tracker.removeCommonProperty("is_pro")
    tracker.trackScreen(TestScreen())

    assertEquals(
      listOf(emptyMap(), mapOf("is_pro" to "false"), mapOf("is_pro" to "true"), emptyMap()),
      local.properties,
    )
  }

  @Test
  fun eventPropertiesWinOverCommonOnes() {
    tracker.setCommonProperty("source", "common")

    tracker.trackAction(TestAction("fab"))
    tracker.trackError(IllegalStateException(), "export_person", mapOf("source" to "call"))

    assertEquals(listOf(mapOf("source" to "fab"), mapOf("source" to "call")), local.properties)
  }

  @Test
  fun commonPropertiesDoNotBypassConsent() {
    consent = false
    tracker.setCommonProperty("is_pro", "true")

    trackAll()

    assertTrue(remote.calls.isEmpty())
    assertEquals(4, local.properties.size)
  }
}

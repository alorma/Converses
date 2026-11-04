package com.alorma.contactnotes.data.tracking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
  private val consent = MutableStateFlow<Boolean?>(false)
  private val tracker = EventTracker(listOf(local, remote), consent, CoroutineScope(UnconfinedTestDispatcher()))

  private fun trackAll() {
    tracker.trackScreen(TestScreen())
    tracker.trackEvent(TestEvent())
    tracker.trackAction(TestAction("fab"))
    tracker.trackError(IllegalStateException("secret"), "export_person")
  }

  private val all = listOf("screen:test_screen", "event:test_event", "action:test_action", "error:export_person")

  @Test
  fun withoutConsentOnlyLocalTrackersReceive() {
    consent.value = false

    trackAll()

    assertEquals(all, local.calls)
    assertTrue(remote.calls.isEmpty())
  }

  @Test
  fun withConsentAllTrackersReceive() {
    consent.value = true

    trackAll()

    assertEquals(all, local.calls)
    assertEquals(all, remote.calls)
  }

  @Test
  fun consentIsCheckedOnEveryCall() {
    consent.value = true
    tracker.trackScreen(TestScreen())
    consent.value = false
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
    consent.value = true
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
    consent.value = false
    tracker.setCommonProperty("is_pro", "true")

    trackAll()

    assertTrue(remote.calls.isEmpty())
    assertEquals(4, local.properties.size)
  }

  @Test
  fun undecidedConsentQueuesRemoteCallsButLocalOnesGoAtOnce() {
    consent.value = null

    trackAll()

    assertEquals(all, local.calls)
    assertTrue(remote.calls.isEmpty())
  }

  @Test
  fun yesSendsTheQueueInOrder() {
    consent.value = null
    trackAll()

    consent.value = true

    assertEquals(all, remote.calls)
  }

  @Test
  fun noDropsTheQueue() {
    consent.value = null
    trackAll()

    consent.value = false
    consent.value = true

    assertTrue(remote.calls.isEmpty())
  }

  @Test
  fun queuedCallsKeepTheCommonPropertiesTheyHadWhenTracked() {
    consent.value = null
    tracker.setCommonProperty("is_pro", "false")
    tracker.trackScreen(TestScreen())
    tracker.setCommonProperty("is_pro", "true")

    consent.value = true

    assertEquals(listOf(mapOf("is_pro" to "false")), remote.properties)
  }

  @Test
  fun queueIsSentBeforeANewCallEvenIfConsentChangedWithoutBeingCollected() {
    val unsettled = MutableStateFlow<Boolean?>(null)
    val remoteOnly = RecordingTracker(isRemote = true)
    // Un scope cancel·lat no executa mai el collect: només la crida següent pot enviar la cua
    val lazyTracker = EventTracker(listOf(remoteOnly), unsettled, CoroutineScope(Job().apply { cancel() }))

    lazyTracker.trackScreen(TestScreen())
    unsettled.value = true
    lazyTracker.trackEvent(TestEvent())

    assertEquals(listOf("screen:test_screen", "event:test_event"), remoteOnly.calls)
  }

  @Test
  fun queueKeepsOnlyTheNewestCallsOverTheLimit() {
    consent.value = null
    repeat(EventTracker.MAX_PENDING) { tracker.trackScreen(TestScreen()) }
    tracker.trackEvent(TestEvent())

    consent.value = true

    assertEquals(EventTracker.MAX_PENDING, remote.calls.size)
    assertEquals("event:test_event", remote.calls.last())
  }

  @Test
  fun aDecisionAlreadyTakenQueuesNothing() {
    consent.value = false
    trackAll()

    consent.value = true

    assertTrue(remote.calls.isEmpty())
  }
}

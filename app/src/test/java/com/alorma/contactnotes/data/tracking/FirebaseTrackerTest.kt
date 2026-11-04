package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.testing.FakeFirebaseGateway
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseTrackerTest {
  private class TestScreen : Screen("person", 2, mapOf("mode" to "edit"))

  private class TestAction : Action("entry_saved", 1, mapOf("kind" to "said"))

  private class TestEvent : Event("app_locked", 3)

  private class RecordingSink : FirebaseSink {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    val errors = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun logEvent(
      name: String,
      params: Map<String, String>,
    ) {
      events += name to params
    }

    override fun recordError(
      error: Throwable,
      keys: Map<String, String>,
    ) {
      errors += error to keys
    }
  }

  private val gateway = FakeFirebaseGateway()
  private val sink = RecordingSink()
  private val tracker = FirebaseTracker(gateway, sink)

  @Test
  fun isRemote() {
    assertTrue(tracker.isRemote)
  }

  @Test
  fun screenIsAScreenView() {
    tracker.trackScreen(TestScreen())

    assertEquals(
      listOf("screen_view" to mapOf("mode" to "edit", "screen_name" to "person", "event_version" to "2")),
      sink.events,
    )
  }

  @Test
  fun actionAndEventKeepTheirName() {
    tracker.trackAction(TestAction())
    tracker.trackEvent(TestEvent())

    assertEquals(
      listOf(
        "entry_saved" to mapOf("kind" to "said", "event_version" to "1"),
        "app_locked" to mapOf("event_version" to "3"),
      ),
      sink.events,
    )
  }

  @Test
  fun firebaseIsEnabledBeforeSending() {
    assertFalse(gateway.enabled)

    tracker.trackAction(TestAction())

    assertTrue(gateway.enabled)
  }

  @Test
  fun errorGoesWithWhereAndPropertiesAsKeys() {
    tracker.trackError(IOException("boom"), "pro_price", mapOf("is_pro" to "false"))

    val (_, keys) = sink.errors.single()
    assertEquals(mapOf("is_pro" to "false", "where" to "pro_price"), keys)
    assertTrue(gateway.enabled)
  }

  @Test
  fun errorMessageNeverLeaves() {
    val original = IOException("Ada said something private", IllegalStateException("Ada's note"))

    tracker.trackError(original, "open_link")

    val (sent, _) = sink.errors.single()
    assertEquals("java.io.IOException", sent.message)
    assertEquals("java.lang.IllegalStateException", sent.cause?.message)
    assertFalse(sent.toString().contains("Ada"))
    assertFalse(sent.cause.toString().contains("Ada"))
  }

  @Test
  fun sanitizedErrorKeepsTheStackTrace() {
    val original = IOException("private")

    val sanitized = FirebaseTracker.sanitize(original)

    assertEquals(original.stackTrace.toList(), sanitized.stackTrace.toList())
    assertNull(sanitized.cause)
  }

  @Test
  fun longCauseChainIsCut() {
    val original = (1..50).fold(IOException("private") as Throwable) { cause, _ -> IllegalStateException(cause) }

    val sanitized = FirebaseTracker.sanitize(original)

    val depth = generateSequence(sanitized) { it.cause }.count()
    assertEquals(11, depth)
  }
}

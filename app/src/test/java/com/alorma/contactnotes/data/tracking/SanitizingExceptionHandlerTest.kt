package com.alorma.contactnotes.data.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class SanitizingExceptionHandlerTest {
  private val received = mutableListOf<Pair<Thread, Throwable>>()
  private val delegate = Thread.UncaughtExceptionHandler { thread, error -> received += thread to error }

  @Test
  fun delegateGetsTheCrashWithoutMessage() {
    val original = IllegalStateException("Ada's private note", RuntimeException("more private text"))
    val thread = Thread.currentThread()

    SanitizingExceptionHandler(delegate).uncaughtException(thread, original)

    val (sentThread, sent) = received.single()
    assertSame(thread, sentThread)
    assertEquals("java.lang.IllegalStateException", sent.message)
    assertEquals(original.stackTrace.toList(), sent.stackTrace.toList())
    assertFalse(generateSequence(sent) { it.cause }.any { it.toString().contains("private") })
  }

  @Test
  fun originalGoesOnlyToTheLocalLog() {
    val original = IllegalStateException("private")
    val logged = mutableListOf<Throwable>()

    SanitizingExceptionHandler(delegate) { logged += it }.uncaughtException(Thread.currentThread(), original)

    assertSame(original, logged.single())
  }

  @Test
  fun failingLogStillReachesTheDelegate() {
    SanitizingExceptionHandler(delegate) { error("log broke") }
      .uncaughtException(Thread.currentThread(), IllegalStateException("private"))

    assertEquals(1, received.size)
  }
}

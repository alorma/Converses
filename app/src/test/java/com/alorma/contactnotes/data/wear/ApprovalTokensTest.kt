package com.alorma.contactnotes.data.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalTokensTest {
  private var now = 10_000L
  private val tokens = ApprovalTokens(clock = { now }, ttlMillis = 5_000)

  @Test
  fun aTokenIsValidOnlyForItsPersonAndUntilItExpires() {
    val token = tokens.issue(personId = 1)

    assertTrue(tokens.isValid(token.value, 1))
    assertFalse(tokens.isValid(token.value, 2))
    now += 5_000
    assertFalse(tokens.isValid(token.value, 1))
  }

  @Test
  fun unknownOrMissingTokensAreInvalid() {
    assertFalse(tokens.isValid(null, 1))
    assertFalse(tokens.isValid("inventat", 1))
  }

  @Test
  fun everyApprovalGetsItsOwnToken() {
    val a = tokens.issue(1)
    val b = tokens.issue(1)

    assertNotEquals(a.value, b.value)
    assertEquals(15_000L, a.expiresAt)
  }
}

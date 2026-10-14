package com.alorma.contactnotes.ui.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedTextTest {
  private fun send(
    text: String? = null,
    subject: String? = null,
  ) = parseSharedText(Intent.ACTION_SEND, text, subject, processText = null)

  @Test
  fun `send keeps the text trimmed`() {
    assertEquals("hello", send(text = "  hello \n"))
  }

  @Test
  fun `send puts the subject in front`() {
    assertEquals("Title\n\nBody", send(text = "Body", subject = "Title"))
  }

  @Test
  fun `send skips a subject already inside the text`() {
    assertEquals("Title - https://x.org", send(text = "Title - https://x.org", subject = "Title"))
  }

  @Test
  fun `send with only a subject uses it`() {
    assertEquals("Title", send(subject = "Title"))
  }

  @Test
  fun `empty or blank send gives nothing`() {
    assertNull(send())
    assertNull(send(text = "   "))
  }

  @Test
  fun `process text reads its own extra and ignores the subject`() {
    assertEquals("picked", parseSharedText(Intent.ACTION_PROCESS_TEXT, "other", "subject", " picked "))
  }

  @Test
  fun `other actions are not imports`() {
    assertNull(parseSharedText(Intent.ACTION_MAIN, "text", null, null))
    assertNull(parseSharedText(null, "text", null, null))
  }

  @Test
  fun `only a single link counts as a url`() {
    assertTrue(isOnlyUrl("https://example.com/a?b=1"))
    assertTrue(isOnlyUrl("  HTTP://example.com "))
    assertFalse(isOnlyUrl("look https://example.com"))
    assertFalse(isOnlyUrl("https://example.com and more"))
    assertFalse(isOnlyUrl("https://"))
    assertFalse(isOnlyUrl("example.com"))
    assertFalse(isOnlyUrl(""))
  }

  @Test
  fun `shared text opens the quick note alone`() {
    assertEquals(listOf<NavKey>(QuickNoteRoute(sharedText = "hi")), sharedTextBackStack("hi"))
  }

  @Test
  fun `no shared text means a normal launch`() {
    assertNull(sharedTextBackStack(null))
  }
}

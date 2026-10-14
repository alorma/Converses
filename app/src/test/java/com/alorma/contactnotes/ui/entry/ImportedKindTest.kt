package com.alorma.contactnotes.ui.entry

import com.alorma.contactnotes.shared.EntryKind
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportedKindTest {
  @Test
  fun `text without a person stays on me`() {
    assertEquals(EntryKind.SAID, importedKind("hello", hasPerson = false))
  }

  @Test
  fun `text with a person is what they said`() {
    assertEquals(EntryKind.HEARD, importedKind("hello", hasPerson = true))
  }

  @Test
  fun `a lone link is a note with or without a person`() {
    assertEquals(EntryKind.HAPPENED, importedKind("https://example.com", hasPerson = false))
    assertEquals(EntryKind.HAPPENED, importedKind("https://example.com", hasPerson = true))
  }

  @Test
  fun `a link inside a sentence is not a lone link`() {
    assertEquals(EntryKind.HEARD, importedKind("look https://example.com", hasPerson = true))
  }

  @Test
  fun `picking who by hand marks the form as touched`() {
    val form = EntryFormState()

    assertEquals(false, form.kindTouched)
    form.kindTouched = true
    assertEquals(true, form.kindTouched)
  }
}

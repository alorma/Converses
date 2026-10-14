package com.alorma.contactnotes.ui

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class DateFormattersTest {
  @Test
  fun reusesTheFormatterForTheSamePatternAndLocale() {
    assertSame(dateFormatter("HH:mm", Locale.ENGLISH), dateFormatter("HH:mm", Locale.ENGLISH))
  }

  @Test
  fun keepsAFormatterPerLocale() {
    val date = LocalDate.of(2026, 10, 6)
    val english = dateFormatter("d MMMM", Locale.ENGLISH)
    val spanish = dateFormatter("d MMMM", Locale.forLanguageTag("es"))

    assertNotSame(english, spanish)
    assertEquals("6 October", date.format(english))
    assertEquals("6 octubre", date.format(spanish))
  }
}

package com.alorma.contactnotes.ui.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class WhenChoiceTest {
  private val zone = ZoneId.of("Europe/Madrid")
  private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
    LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

  @Test
  fun nowResolvesAtSaveTime() {
    assertNull(WhenChoice.Now.at)
  }

  @Test
  fun hourAgoIsOneHourBefore() {
    val now = millis(2026, 10, 6, 21, 46)
    assertEquals(millis(2026, 10, 6, 20, 46), WhenChoice.hourAgo(now).at)
  }

  @Test
  fun yesterdayKeepsTheTimeOfDay() {
    val now = millis(2026, 10, 6, 21, 46)
    assertEquals(millis(2026, 10, 5, 21, 46), WhenChoice.yesterday(now, zone).at)
  }

  @Test
  fun yesterdayKeepsTheTimeOfDayAcrossDstChange() {
    val now = millis(2026, 10, 26, 9, 15)
    assertEquals(millis(2026, 10, 25, 9, 15), WhenChoice.yesterday(now, zone).at)
  }

  @Test
  fun encodeDecodeRoundTrips() {
    listOf(
      WhenChoice.Now,
      WhenChoice.HourAgo(1),
      WhenChoice.Yesterday(2),
      WhenChoice.Custom(3),
      WhenChoice.Stored(4),
    ).forEach { assertEquals(it, WhenChoice.decode(WhenChoice.encode(it))) }
  }

  @Test
  fun longDateStartsWithCapitalizedWeekday() {
    val monday = millis(2026, 10, 5, 21, 46)
    val english = formatLongDate(monday, zone, Locale.ENGLISH)
    assertTrue(english, english.startsWith("Monday 5"))
    val catalan = formatLongDate(monday, zone, Locale.forLanguageTag("ca"))
    assertTrue(catalan, catalan.startsWith("Dilluns 5") && catalan.contains("oct"))
    val spanish = formatLongDate(monday, zone, Locale.forLanguageTag("es"))
    assertTrue(spanish, spanish.startsWith("Lunes 5") && spanish.contains("oct"))
  }

  @Test
  fun dateTimeJoinsDateAndTime() {
    val at = millis(2026, 10, 5, 21, 46)
    assertTrue(formatDateTime(at, zone, Locale.ENGLISH).endsWith(" · 21:46"))
  }
}

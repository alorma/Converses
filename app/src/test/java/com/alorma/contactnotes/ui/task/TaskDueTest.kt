package com.alorma.contactnotes.ui.task

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDueTest {
  // Dilluns 2 de novembre de 2026
  private val monday = LocalDate.of(2026, 11, 2)

  @Test
  fun theRelevantDayIsTheNearestFridayOrMondayAtLeastTwoDaysAway() {
    val expected =
      mapOf(
        DayOfWeek.MONDAY to DayOfWeek.FRIDAY,
        DayOfWeek.TUESDAY to DayOfWeek.FRIDAY,
        DayOfWeek.WEDNESDAY to DayOfWeek.FRIDAY,
        DayOfWeek.THURSDAY to DayOfWeek.MONDAY,
        DayOfWeek.FRIDAY to DayOfWeek.MONDAY,
        DayOfWeek.SATURDAY to DayOfWeek.MONDAY,
        DayOfWeek.SUNDAY to DayOfWeek.FRIDAY,
      )
    (0L..6L).map(monday::plusDays).forEach { today ->
      val day = relevantDay(today)
      assertEquals(today.dayOfWeek.name, expected.getValue(today.dayOfWeek), day.dayOfWeek)
      // Mai coincideix amb Avui, Demà ni La setmana que ve
      assertTrue(ChronoUnit.DAYS.between(today, day) in 2..5)
    }
  }

  @Test
  fun everyPresetHasItsOwnDate() {
    assertNull(DuePreset.None.date(monday))
    assertEquals(monday, DuePreset.Today.date(monday))
    assertEquals(monday.plusDays(1), DuePreset.Tomorrow.date(monday))
    assertEquals(LocalDate.of(2026, 11, 6), DuePreset.RelevantDay.date(monday))
    assertEquals(monday.plusWeeks(1), DuePreset.NextWeek.date(monday))
  }

  @Test
  fun aDateIsMatchedToItsPresetOrNoneForAnyOtherDay() {
    DuePreset.entries.forEach { assertEquals(it, presetOf(it.date(monday), monday)) }
    assertNull(presetOf(monday.plusDays(20), monday))
    // Una tasca passada de data no és «Avui»
    assertNull(presetOf(monday.minusDays(1), monday))
  }

  @Test
  fun theDayNameStartsWithACapital() {
    assertEquals("Divendres", dayName(LocalDate.of(2026, 11, 6), Locale.forLanguageTag("ca")))
    assertEquals("Friday", dayName(LocalDate.of(2026, 11, 6), Locale.ENGLISH))
  }
}

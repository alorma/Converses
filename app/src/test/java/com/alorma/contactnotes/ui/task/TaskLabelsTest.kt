package com.alorma.contactnotes.ui.task

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskLabelsTest {
  private val today = LocalDate.of(2026, 11, 2)

  @Test
  fun dueLabelsLookForwardAndKeepOverdueOnesNeutral() {
    assertEquals(DueLabel.None, dueLabel(null, today))
    assertEquals(DueLabel.Today, dueLabel(today, today))
    assertEquals(DueLabel.Tomorrow, dueLabel(today.plusDays(1), today))
    assertEquals(DueLabel.Weekday(today.plusDays(2)), dueLabel(today.plusDays(2), today))
    assertEquals(DueLabel.Weekday(today.plusDays(6)), dueLabel(today.plusDays(6), today))
    assertEquals(DueLabel.Date(today.plusDays(7)), dueLabel(today.plusDays(7), today))
    assertEquals(DueLabel.Yesterday, dueLabel(today.minusDays(1), today))
    assertEquals(DueLabel.Overdue(3), dueLabel(today.minusDays(3), today))
  }

  @Test
  fun doneLabelsSayTodayYesterdayOrTheDate() {
    assertEquals(DoneLabel.Today, doneLabel(today, today))
    assertEquals(DoneLabel.Yesterday, doneLabel(today.minusDays(1), today))
    assertEquals(DoneLabel.On(LocalDate.of(2026, 5, 3)), doneLabel(LocalDate.of(2026, 5, 3), today))
  }
}

package com.alorma.contactnotes.ui.task

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Etiqueta de data d'una tasca pendent (9d). Les passades de data no es pinten de vermell. */
sealed interface DueLabel {
  data object None : DueLabel

  data object Today : DueLabel

  data object Tomorrow : DueLabel

  data object Yesterday : DueLabel

  /** D'aquí a 2–6 dies: el nom del dia («DIVENDRES»). */
  data class Weekday(
    val date: LocalDate,
  ) : DueLabel

  /** D'aquí a una setmana o més: la data. */
  data class Date(
    val date: LocalDate,
  ) : DueLabel

  /** Fa dos dies o més («FA 3 DIES»). */
  data class Overdue(
    val days: Int,
  ) : DueLabel
}

fun dueLabel(
  dueDate: LocalDate?,
  today: LocalDate,
): DueLabel {
  if (dueDate == null) return DueLabel.None
  val days = ChronoUnit.DAYS.between(today, dueDate)
  return when {
    days == 0L -> DueLabel.Today
    days == 1L -> DueLabel.Tomorrow
    days == -1L -> DueLabel.Yesterday
    days < -1L -> DueLabel.Overdue((-days).toInt())
    days < 7L -> DueLabel.Weekday(dueDate)
    else -> DueLabel.Date(dueDate)
  }
}

/** Etiqueta d'una tasca feta (9f): «FET AVUI», «FET AHIR» o «FET EL 3 DE MAIG». */
sealed interface DoneLabel {
  data object Today : DoneLabel

  data object Yesterday : DoneLabel

  data class On(
    val date: LocalDate,
  ) : DoneLabel
}

fun doneLabel(
  doneOn: LocalDate,
  today: LocalDate,
): DoneLabel =
  when (ChronoUnit.DAYS.between(doneOn, today)) {
    0L -> DoneLabel.Today
    1L -> DoneLabel.Yesterday
    else -> DoneLabel.On(doneOn)
  }

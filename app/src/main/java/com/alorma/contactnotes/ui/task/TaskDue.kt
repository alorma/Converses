package com.alorma.contactnotes.ui.task

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Xips fixos de «Quan» d'una tasca, sempre cap al futur. «Tria dia…» no hi és: és qualsevol altra data. */
enum class DuePreset { None, Today, Tomorrow, RelevantDay, NextWeek }

/**
 * Proper dia rellevant (el xip amb el nom del dia): el divendres o el dilluns més proper que caigui com a mínim
 * d'aquí a dos dies, perquè no coincideixi mai amb «Avui», «Demà» ni «La setmana que ve» (d'aquí a set dies).
 */
fun relevantDay(today: LocalDate): LocalDate {
  val start = today.plusDays(2)
  val friday = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
  val monday = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
  return minOf(friday, monday)
}

fun DuePreset.date(today: LocalDate): LocalDate? =
  when (this) {
    DuePreset.None -> null
    DuePreset.Today -> today
    DuePreset.Tomorrow -> today.plusDays(1)
    DuePreset.RelevantDay -> relevantDay(today)
    DuePreset.NextWeek -> today.plusWeeks(1)
  }

/** El xip que correspon a [date]; `null` si és una altra data (es mostra a «Tria dia…»). */
fun presetOf(
  date: LocalDate?,
  today: LocalDate,
): DuePreset? = DuePreset.entries.firstOrNull { it.date(today) == date }

/** «Divendres»: el nom del dia, amb la primera lletra en majúscula. */
fun dayName(
  date: LocalDate,
  locale: Locale = Locale.getDefault(),
): String = date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

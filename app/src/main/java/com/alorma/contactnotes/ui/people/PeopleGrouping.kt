package com.alorma.contactnotes.ui.people

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.ui.dateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/** Període d'activitat sota el qual es llisten les persones. */
sealed interface ActivityPeriod {
  data object Today : ActivityPeriod

  data object Yesterday : ActivityPeriod

  data object ThisWeek : ActivityPeriod

  data class Month(
    val month: YearMonth,
  ) : ActivityPeriod
}

data class PeopleGroup(
  val period: ActivityPeriod,
  val people: List<PersonListItem>,
)

/**
 * Agrupa per data de l'última activitat. La llista ja ve ordenada de més recent a més antiga, així que els grups
 * surten en ordre d'aparició i dins de cada grup es manté l'ordre d'entrada. La setmana comença segons [locale].
 */
fun List<PersonListItem>.groupByActivity(
  today: LocalDate = LocalDate.now(),
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): List<PeopleGroup> {
  val weekStart = today.with(WeekFields.of(locale).dayOfWeek(), 1)
  return groupBy { item ->
    val day = Instant.ofEpochMilli(item.lastActivityAt).atZone(zone).toLocalDate()
    when {
      day >= today -> ActivityPeriod.Today
      day == today.minusDays(1) -> ActivityPeriod.Yesterday
      day >= weekStart -> ActivityPeriod.ThisWeek
      else -> ActivityPeriod.Month(YearMonth.from(day))
    }
  }.map { (period, people) -> PeopleGroup(period, people) }
}

/** «Novembre» per a l'any en curs, «Novembre de 2025» (patrons per llengua) per als anteriors; amb inicial majúscula. */
internal fun monthLabel(
  month: YearMonth,
  today: LocalDate,
  locale: Locale,
  currentYearPattern: String,
  otherYearPattern: String,
): String {
  val pattern = if (month.year == today.year) currentYearPattern else otherYearPattern
  return dateFormatter(pattern, locale).format(month).replaceFirstChar { it.titlecase(locale) }
}

@Composable
fun ActivityPeriod.label(): String =
  when (this) {
    ActivityPeriod.Today -> stringResource(R.string.group_today)
    ActivityPeriod.Yesterday -> stringResource(R.string.group_yesterday)
    ActivityPeriod.ThisWeek -> stringResource(R.string.group_this_week)
    is ActivityPeriod.Month ->
      monthLabel(
        month = month,
        today = LocalDate.now(),
        locale = LocalConfiguration.current.locales[0],
        currentYearPattern = stringResource(R.string.group_month_pattern),
        otherYearPattern = stringResource(R.string.group_month_other_year_pattern),
      )
  }

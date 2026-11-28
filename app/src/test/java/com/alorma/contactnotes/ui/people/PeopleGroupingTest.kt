package com.alorma.contactnotes.ui.people

import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleGroupingTest {
  private val ca = Locale.forLanguageTag("ca")
  private val thursday = LocalDate.of(2026, 11, 26)

  private fun item(
    name: String,
    day: LocalDate,
    protected: Boolean = false,
  ) = PersonListItem(
    person = PersonEntity(name = name, isProtected = protected, createdAt = 0, updatedAt = 0),
    lastEntry = null,
    channels = emptyList(),
    lastActivityAt = day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
  )

  private fun List<PersonListItem>.group(
    today: LocalDate = thursday,
    locale: Locale = ca,
  ) = groupByActivity(today, ZoneOffset.UTC, locale)

  private fun List<PeopleGroup>.names() = map { g -> g.period to g.people.map { it.person.name } }

  @Test
  fun groupsTodayYesterdayWeekAndMonths() {
    val list =
      listOf(
        item("A", LocalDate.of(2026, 11, 26)),
        item("B", LocalDate.of(2026, 11, 25)),
        item("C", LocalDate.of(2026, 11, 23)),
        item("D", LocalDate.of(2026, 11, 20)),
        item("E", LocalDate.of(2025, 12, 3)),
      )
    assertEquals(
      listOf(
        ActivityPeriod.Today to listOf("A"),
        ActivityPeriod.Yesterday to listOf("B"),
        ActivityPeriod.ThisWeek to listOf("C"),
        ActivityPeriod.Month(YearMonth.of(2026, 11)) to listOf("D"),
        ActivityPeriod.Month(YearMonth.of(2025, 12)) to listOf("E"),
      ),
      list.group().names(),
    )
  }

  @Test
  fun keepsTheIncomingOrderInsideAGroup() {
    val list = listOf(item("Z", thursday), item("A", thursday))
    assertEquals(listOf(ActivityPeriod.Today to listOf("Z", "A")), list.group().names())
  }

  @Test
  fun futureActivityIsToday() {
    val list = listOf(item("A", LocalDate.of(2026, 11, 28)))
    assertEquals(listOf(ActivityPeriod.Today to listOf("A")), list.group().names())
  }

  @Test
  fun mondayHasNoThisWeekGroup() {
    val list = listOf(item("A", LocalDate.of(2026, 11, 22)), item("B", LocalDate.of(2026, 11, 20)))
    val groups = list.group(today = LocalDate.of(2026, 11, 23))
    assertEquals(
      listOf(
        ActivityPeriod.Yesterday to listOf("A"),
        ActivityPeriod.Month(YearMonth.of(2026, 11)) to listOf("B"),
      ),
      groups.names(),
    )
    assertTrue(groups.none { it.period == ActivityPeriod.ThisWeek })
  }

  @Test
  fun weekStartsOnSundayInEnglish() {
    val list = listOf(item("A", LocalDate.of(2026, 11, 22)))
    val groups = list.group(today = LocalDate.of(2026, 11, 24), locale = Locale.US)
    assertEquals(listOf(ActivityPeriod.ThisWeek to listOf("A")), groups.names())
  }

  @Test
  fun protectedPeopleUseTheirActivityTime() {
    val list = listOf(item("P", LocalDate.of(2026, 11, 25), protected = true))
    assertEquals(listOf(ActivityPeriod.Yesterday to listOf("P")), list.group().names())
  }

  @Test
  fun emptyListHasNoGroups() {
    assertTrue(emptyList<PersonListItem>().group().isEmpty())
  }

  @Test
  fun monthLabelOmitsTheYearOnlyForTheCurrentYear() {
    val today = thursday

    fun label(
      month: YearMonth,
      locale: Locale,
      other: String,
    ) = monthLabel(month, today, locale, "LLLL", other)

    assertEquals("Novembre", label(YearMonth.of(2026, 11), ca, "LLLL 'de' yyyy"))
    assertEquals("Desembre de 2025", label(YearMonth.of(2025, 12), ca, "LLLL 'de' yyyy"))
    assertEquals("November 2025", label(YearMonth.of(2025, 11), Locale.US, "LLLL yyyy"))
  }
}

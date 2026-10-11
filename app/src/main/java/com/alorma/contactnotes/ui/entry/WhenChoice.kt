package com.alorma.contactnotes.ui.entry

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Opció triada al full «Quan va ser?». `at == null` vol dir «ara», que es resol en desar. */
sealed interface WhenChoice {
  val at: Long?

  data object Now : WhenChoice {
    override val at: Long? = null
  }

  data class HourAgo(
    override val at: Long,
  ) : WhenChoice

  data class Yesterday(
    override val at: Long,
  ) : WhenChoice

  data class Custom(
    override val at: Long,
  ) : WhenChoice

  /** Data desada d'una entrada que s'edita; no marca cap opció. */
  data class Stored(
    override val at: Long,
  ) : WhenChoice

  companion object {
    fun hourAgo(now: Long): HourAgo = HourAgo(now - 60 * 60 * 1000L)

    fun yesterday(
      now: Long,
      zone: ZoneId = ZoneId.systemDefault(),
    ): Yesterday =
      Yesterday(
        Instant
          .ofEpochMilli(now)
          .atZone(zone)
          .minusDays(1)
          .toInstant()
          .toEpochMilli(),
      )

    fun encode(choice: WhenChoice): List<Any?> =
      listOf(
        when (choice) {
          Now -> "now"
          is HourAgo -> "hour_ago"
          is Yesterday -> "yesterday"
          is Custom -> "custom"
          is Stored -> "stored"
        },
        choice.at,
      )

    fun decode(saved: List<Any?>): WhenChoice {
      val at = saved[1] as Long?
      return when (saved[0] as String) {
        "hour_ago" -> HourAgo(at!!)
        "yesterday" -> Yesterday(at!!)
        "custom" -> Custom(at!!)
        "stored" -> Stored(at!!)
        else -> Now
      }
    }
  }
}

/** «Dilluns 6 d'oct.»: dia de la setmana, dia i mes abreujat, amb la primera lletra en majúscula. */
fun formatLongDate(
  epochMillis: Long,
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): String =
  Instant
    .ofEpochMilli(epochMillis)
    .atZone(zone)
    .format(DateTimeFormatter.ofPattern("EEEE d MMM", locale))
    .replaceFirstChar { it.titlecase(locale) }

fun formatTimeOfDay(
  epochMillis: Long,
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", locale))

/** «Dilluns 6 d'oct. · 21:46». */
fun formatDateTime(
  epochMillis: Long,
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): String = "${formatLongDate(epochMillis, zone, locale)} · ${formatTimeOfDay(epochMillis, zone, locale)}"

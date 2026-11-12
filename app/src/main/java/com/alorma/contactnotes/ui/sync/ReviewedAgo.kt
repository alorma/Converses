package com.alorma.contactnotes.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R

/** Quant fa que es van revisar les paraules, en la unitat més gran que hi cap. */
sealed interface ReviewedAgo {
  data object Today : ReviewedAgo

  data class Days(
    val count: Int,
  ) : ReviewedAgo

  data class Months(
    val count: Int,
  ) : ReviewedAgo

  data class Years(
    val count: Int,
  ) : ReviewedAgo
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_YEAR = 365

/** Menys de 24 h és avui; 1–29 dies, dies; fins a 11 mesos (dies / 30), mesos; a partir d'un any (dies / 365), anys. */
fun reviewedAgo(
  reviewedAt: Long,
  now: Long,
): ReviewedAgo {
  val days = ((now - reviewedAt) / DAY_MILLIS).coerceAtLeast(0).toInt()
  return when {
    days < 1 -> ReviewedAgo.Today
    days < DAYS_PER_MONTH -> ReviewedAgo.Days(days)
    days < DAYS_PER_YEAR -> ReviewedAgo.Months((days / DAYS_PER_MONTH).coerceAtMost(11))
    else -> ReviewedAgo.Years(days / DAYS_PER_YEAR)
  }
}

@Composable
fun ReviewedAgo.label(): String =
  when (this) {
    ReviewedAgo.Today -> stringResource(R.string.sync_words_reviewed_today)
    is ReviewedAgo.Days -> pluralStringResource(R.plurals.sync_words_reviewed_days, count, count)
    is ReviewedAgo.Months -> pluralStringResource(R.plurals.sync_words_reviewed_months, count, count)
    is ReviewedAgo.Years -> pluralStringResource(R.plurals.sync_words_reviewed_years, count, count)
  }

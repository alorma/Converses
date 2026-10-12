package com.alorma.contactnotes.ui.person

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.ui.people.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun ChannelType.icon(): ImageVector =
  when (this) {
    ChannelType.IN_PERSON -> Icons.Rounded.Group
    ChannelType.PHONE -> Icons.Rounded.Call
    ChannelType.CHAT -> Icons.AutoMirrored.Rounded.Chat
    ChannelType.SOCIAL -> Icons.Rounded.Public
    ChannelType.OTHER -> Icons.Rounded.MoreHoriz
  }

fun entryDate(
  epochMillis: Long,
  zone: ZoneId = ZoneId.systemDefault(),
): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun formatTime(
  epochMillis: Long,
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", locale))

fun formatMonthYear(
  epochMillis: Long,
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

/** "Avui", "Ahir" o la data amb l'any només si no és l'actual. */
@Composable
fun dayLabel(
  date: LocalDate,
  today: LocalDate = LocalDate.now(),
  locale: Locale = Locale.getDefault(),
): String {
  val days = ChronoUnit.DAYS.between(date, today)
  return when {
    days == 0L -> stringResource(R.string.today)
    days == 1L -> stringResource(R.string.yesterday)
    date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
  }
}

@Composable
fun channelName(type: ChannelType): String = stringResource(type.label())

fun channelName(
  context: Context,
  type: ChannelType,
): String = context.getString(type.label())

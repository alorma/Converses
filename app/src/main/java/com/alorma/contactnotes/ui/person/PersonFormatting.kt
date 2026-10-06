package com.alorma.contactnotes.ui.person

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.designsystem.components.TimelineMarkerStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@StringRes
fun EntryKind.label(): Int = when (this) {
  EntryKind.SAID -> R.string.kind_said
  EntryKind.HEARD -> R.string.kind_heard
  EntryKind.HAPPENED -> R.string.kind_happened
}

fun EntryKind.icon(): ImageVector = when (this) {
  EntryKind.SAID -> Icons.Rounded.NorthEast
  EntryKind.HEARD -> Icons.Rounded.SouthWest
  EntryKind.HAPPENED -> Icons.Rounded.Event
}

fun EntryKind.markerStyle(): TimelineMarkerStyle = when (this) {
  EntryKind.SAID -> TimelineMarkerStyle.Filled
  EntryKind.HEARD -> TimelineMarkerStyle.Tonal
  EntryKind.HAPPENED -> TimelineMarkerStyle.Outlined
}

fun ChannelType.icon(): ImageVector = when (this) {
  ChannelType.DISCORD -> Icons.Rounded.Forum
  ChannelType.TELEGRAM -> Icons.Rounded.Send
  ChannelType.WHATSAPP -> Icons.Rounded.Chat
  ChannelType.REDDIT -> Icons.Rounded.Public
  ChannelType.PHONE -> Icons.Rounded.Call
  ChannelType.IN_PERSON -> Icons.Rounded.People
  ChannelType.OTHER -> Icons.Rounded.MoreHoriz
}

fun entryDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
  Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
  Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", locale))

fun formatMonthYear(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
  Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

/** "Avui", "Ahir" o la data amb l'any només si no és l'actual. */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
  val days = ChronoUnit.DAYS.between(date, today)
  return when {
    days == 0L -> stringResource(R.string.today)
    days == 1L -> stringResource(R.string.yesterday)
    date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
  }
}

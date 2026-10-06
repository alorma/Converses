package com.alorma.contactnotes.ui.people

import androidx.annotation.StringRes
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@StringRes
fun ChannelFilter.label(): Int = when (this) {
  ChannelFilter.All -> R.string.filter_all
  ChannelFilter.Chat -> R.string.filter_chat
  ChannelFilter.Social -> R.string.filter_social
  ChannelFilter.InPerson -> R.string.filter_in_person
  ChannelFilter.Phone -> R.string.filter_phone
}

@StringRes
fun ChannelType.label(): Int = when (this) {
  ChannelType.DISCORD -> R.string.channel_discord
  ChannelType.TELEGRAM -> R.string.channel_telegram
  ChannelType.WHATSAPP -> R.string.channel_whatsapp
  ChannelType.REDDIT -> R.string.channel_reddit
  ChannelType.PHONE -> R.string.channel_phone
  ChannelType.IN_PERSON -> R.string.channel_in_person
  ChannelType.OTHER -> R.string.channel_other
}

fun initialsOf(name: String): String =
  name.split(' ', '_', '-', '.')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }

/** Hora si és d'avui, dia abreujat en minúscula els últims 6 dies, data curta aquest any i data completa abans. */
fun formatLastActivity(
  epochMillis: Long,
  today: LocalDate = LocalDate.now(),
  zone: ZoneId = ZoneId.systemDefault(),
  locale: Locale = Locale.getDefault(),
): String {
  val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
  val days = ChronoUnit.DAYS.between(dateTime.toLocalDate(), today)
  return when {
    days <= 0 -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    days < 7 -> dateTime.format(DateTimeFormatter.ofPattern("EEE", locale)).lowercase(locale)
    dateTime.year == today.year -> dateTime.format(DateTimeFormatter.ofPattern("d MMM", locale))
    else -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))
  }
}

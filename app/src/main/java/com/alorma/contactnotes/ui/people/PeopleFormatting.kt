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
fun ChannelGroup.label(): Int = when (this) {
  ChannelGroup.Chat -> R.string.filter_chat
  ChannelGroup.Social -> R.string.filter_social
  ChannelGroup.InPerson -> R.string.filter_in_person
  ChannelGroup.Phone -> R.string.filter_phone
}

@StringRes
fun ChannelType.label(): Int = when (this) {
  ChannelType.IN_PERSON -> R.string.channel_in_person
  ChannelType.WHATSAPP -> R.string.channel_whatsapp
  ChannelType.TELEGRAM -> R.string.channel_telegram
  ChannelType.X -> R.string.channel_x
  ChannelType.INSTAGRAM -> R.string.channel_instagram
  ChannelType.PHONE -> R.string.channel_phone
  ChannelType.EMAIL -> R.string.channel_email
  ChannelType.DISCORD -> R.string.channel_discord
  ChannelType.SIGNAL -> R.string.channel_signal
  ChannelType.LINKEDIN -> R.string.channel_linkedin
  ChannelType.REDDIT -> R.string.channel_reddit
}

fun initialsOf(name: String): String {
  val words = name.split(' ', '_', '-', '.').filter { it.isNotBlank() }
  return listOfNotNull(words.firstOrNull(), words.lastOrNull()?.takeIf { words.size > 1 })
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }
}

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

/** Nom del color d'un `AvatarColor`. */
@StringRes
fun colorNameRes(name: String): Int = when (name) {
  "Lavender" -> R.string.color_lavender
  "Mint" -> R.string.color_mint
  "Peach" -> R.string.color_peach
  "Sky" -> R.string.color_sky
  "Rose" -> R.string.color_rose
  "Lemon" -> R.string.color_lemon
  else -> R.string.color_sand
}

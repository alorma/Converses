package com.alorma.contactnotes.ui.people

import androidx.annotation.StringRes
import com.alorma.contactnotes.R
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.ui.dateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

@StringRes
fun ChannelType.label(): Int =
  when (this) {
    ChannelType.IN_PERSON -> R.string.channel_in_person
    ChannelType.PHONE -> R.string.channel_phone
    ChannelType.CHAT -> R.string.channel_chat
    ChannelType.SOCIAL -> R.string.channel_social
    ChannelType.OTHER -> R.string.channel_other
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
    days <= 0 -> dateTime.format(dateFormatter("HH:mm", locale))
    days < 7 -> dateTime.format(dateFormatter("EEE", locale)).lowercase(locale)
    dateTime.year == today.year -> dateTime.format(dateFormatter("d MMM", locale))
    else -> dateTime.format(dateFormatter("dd/MM/yyyy", locale))
  }
}

/** Nom del color d'un `AvatarColor`. */
@StringRes
fun colorNameRes(name: String): Int =
  when (name) {
    "Lavender" -> R.string.color_lavender
    "Mint" -> R.string.color_mint
    "Peach" -> R.string.color_peach
    "Sky" -> R.string.color_sky
    "Rose" -> R.string.color_rose
    "Lemon" -> R.string.color_lemon
    else -> R.string.color_sand
  }

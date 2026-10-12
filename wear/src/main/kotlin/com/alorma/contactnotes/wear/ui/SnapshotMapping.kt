package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.wear.components.EntryAuthor
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Quan va passar una cosa, sense text: la capa d'Android la tradueix a l'idioma del rellotge. */
sealed interface RelativeTime {
  data object JustNow : RelativeTime

  data class Minutes(
    val count: Int,
  ) : RelativeTime

  data class Hours(
    val count: Int,
  ) : RelativeTime

  data object Yesterday : RelativeTime

  data class Date(
    val timestamp: Long,
  ) : RelativeTime
}

/** Textos que el mapatge necessita. A Android els dona `AndroidWearLabels`; als tests, una versió falsa. */
interface WearLabels {
  val me: String
  val note: String

  fun channel(type: ChannelType): String

  fun relative(time: RelativeTime): String

  /** Hora del dia («21:40»). */
  fun clock(timestamp: Long): String
}

fun relativeTime(
  timestamp: Long,
  now: Long,
  zone: ZoneId = ZoneId.systemDefault(),
): RelativeTime {
  val minutes = ((now - timestamp) / 60_000L).coerceAtLeast(0).toInt()
  val days =
    ChronoUnit.DAYS.between(
      Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate(),
      Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
    )
  return when {
    days >= 2 -> RelativeTime.Date(timestamp)
    days == 1L -> RelativeTime.Yesterday
    minutes < 1 -> RelativeTime.JustNow
    minutes < 60 -> RelativeTime.Minutes(minutes)
    else -> RelativeTime.Hours(minutes / 60)
  }
}

/** Fins a dues inicials: «Luna Roja» → «LR», «Kael» → «K». */
fun initialsOf(name: String): String =
  name
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }

/** Una protegida no duu hora ni entrades: tampoc aquí se'n mostra res més que el nom. */
fun WearPerson.toItem(
  labels: WearLabels,
  now: Long,
  zone: ZoneId = ZoneId.systemDefault(),
): PersonItem =
  PersonItem(
    id = id,
    name = name,
    initials = initialsOf(name),
    color = avatarColor,
    avatarShape = avatarShape,
    lastEntry = if (isProtected) null else lastEntryAt?.let { labels.relative(relativeTime(it, now, zone)) },
    isProtected = isProtected,
  )

/** «Qui · hora · canal» i l'autor, com `whoLabel(...)` del mòbil: Jo / {primer nom} / Apunt. */
fun WearEntry.toItem(
  person: WearPerson,
  labels: WearLabels,
): EntryItem {
  val firstName = person.name.trim().substringBefore(' ')
  val (author, who) =
    when (kind) {
      EntryKind.SAID -> EntryAuthor.Me to labels.me
      EntryKind.HEARD ->
        EntryAuthor.Person(initialsOf(person.name), person.avatarColor, person.avatarShape) to
          firstName
      EntryKind.HAPPENED -> EntryAuthor.Note to labels.note
    }
  return EntryItem(
    id = id,
    author = author,
    meta = "$who · ${labels.clock(occurredAt)} · ${labels.channel(channel)}",
    text = text,
  )
}

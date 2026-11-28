package com.alorma.contactnotes.ui.people

import com.alorma.contactnotes.data.db.PersonListItem

private const val JUST_NOW_MILLIS = 60_000L

/** La persona de l'entrada més recent entre les no protegides; en empat, la primera de la llista. */
fun List<PersonListItem>.featured(): PersonListItem? =
  filter { it.lastEntry != null && !it.person.isProtected }
    .maxByOrNull { it.lastEntry!!.occurredAt }

/** `null` vol dir «Ara mateix» (menys d'un minut o data futura); si no, el moment per mostrar-lo en relatiu. */
fun featuredTimeAt(
  occurredAt: Long,
  now: Long,
): Long? = if (now - occurredAt < JUST_NOW_MILLIS) null else occurredAt

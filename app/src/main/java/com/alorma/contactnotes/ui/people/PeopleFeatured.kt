package com.alorma.contactnotes.ui.people

import com.alorma.contactnotes.data.db.PersonEntity
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

/**
 * El detall que fa de panell buit a pantalla gran no mostra mai una persona protegida (no ha passat per l'empremta)
 * ni una d'arxivada: si la destacada que s'havia desat ha canviat d'estat, queda el panell buit.
 */
fun PersonEntity.hiddenInPlaceholder(): Boolean = isProtected || archived

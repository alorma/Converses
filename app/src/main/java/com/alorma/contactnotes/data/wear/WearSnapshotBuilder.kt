package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.shared.sanitized

/**
 * Converteix les dades de Room en l'instantani del rellotge. Aquí és el filtre de privacitat:
 * les arxivades no hi van, i una protegida només duu `id`, nom, color i forma.
 */
object WearSnapshotBuilder {
  fun build(
    people: List<PersonListItem>,
    recentEntries: List<EntryEntity>,
    appLockActive: Boolean,
    now: Long,
  ): WearSnapshot {
    val entriesByPerson = recentEntries.groupBy { it.personId }
    return WearSnapshot(
      generatedAt = now,
      appLockActive = appLockActive,
      people =
        people
          .filter { !it.person.archived }
          .map { item ->
            val person = item.person
            if (person.isProtected) {
              WearPerson(person.id, person.name, person.color, person.shape, isProtected = true)
            } else {
              WearPerson(
                id = person.id,
                name = person.name,
                avatarColor = person.color,
                avatarShape = person.shape,
                isProtected = false,
                lastEntryAt = item.lastEntry?.occurredAt,
                entries = entriesByPerson[person.id].orEmpty().map { it.toWearEntry() },
              )
            }
          },
    ).sanitized()
  }

  private fun EntryEntity.toWearEntry() = WearEntry(id, kind, channelType, text, occurredAt)
}

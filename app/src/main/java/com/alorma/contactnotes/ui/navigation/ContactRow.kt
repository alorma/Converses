package com.alorma.contactnotes.ui.navigation

import com.alorma.contactnotes.data.db.PersonEntity

/** Les dues files de Converses a l'app Contactes (`docs/contacts`, C9). */
enum class ContactRowAction {
  /** «Veure notes»: la fitxa de la persona. */
  ViewNotes,

  /** «Crear nota»: la fitxa amb el full de nova entrada obert. */
  CreateNote,
}

/**
 * Mimetype de cada fila. Porta l'`applicationId` perquè, amb la versió de debug i la de release instal·lades alhora,
 * cada fila només l'obri la seva app (si no, Contactes demanaria amb quina obrir-la).
 */
fun contactRowMimeType(
  applicationId: String,
  action: ContactRowAction,
): String =
  when (action) {
    ContactRowAction.ViewNotes -> "vnd.android.cursor.item/vnd.$applicationId.person"
    ContactRowAction.CreateNote -> "vnd.android.cursor.item/vnd.$applicationId.note"
  }

fun contactRowAction(
  applicationId: String,
  mimeType: String?,
): ContactRowAction? = ContactRowAction.entries.firstOrNull { contactRowMimeType(applicationId, it) == mimeType }

/**
 * On porta una fila tocada a Contactes: la persona (amb el full obert per a «Crear nota»), el desbloqueig primer si és
 * protegida, o `null` (l'app normal) si el flag està apagat, la fila no és nostra o la persona ja no hi és.
 */
fun contactRowRoute(
  action: ContactRowAction?,
  person: PersonEntity?,
  enabled: Boolean,
): AppRoute? {
  if (!enabled || action == null || person == null) return null
  val openNewEntry = action == ContactRowAction.CreateNote
  return if (person.isProtected) {
    PersonLockedRoute(person.id, openNewEntry)
  } else {
    PersonRoute(person.id, openNewEntry)
  }
}

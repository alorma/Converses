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

/** Extres amb què `ContactRowActivity` obre `MainActivity`: només dades, mai una ruta ja resolta. */
const val EXTRA_CONTACT_ROW_PERSON_ID = "com.alorma.contactnotes.extra.CONTACT_ROW_PERSON_ID"
const val EXTRA_CONTACT_ROW_ACTION = "com.alorma.contactnotes.extra.CONTACT_ROW_ACTION"

/**
 * Pila inicial de `MainActivity` per a una fila de Contactes, o `null` si l'intent no en ve. `MainActivity` és
 * exportada i qualsevol app li pot passar aquests extres, per això es torna a resoldre aquí (flag, persona, protegida)
 * en lloc de confiar en el que digui l'intent.
 */
fun contactRowBackStack(
  actionName: String?,
  personId: Long?,
  enabled: Boolean,
  findPerson: (Long) -> PersonEntity?,
): List<AppRoute>? {
  val action = ContactRowAction.entries.firstOrNull { it.name == actionName } ?: return null
  if (personId == null || !enabled) return null
  return contactRowRoute(action, findPerson(personId), enabled)?.let { listOf(it) }
}

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

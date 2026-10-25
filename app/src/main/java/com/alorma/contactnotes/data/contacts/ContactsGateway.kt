package com.alorma.contactnotes.data.contacts

/** Persona que ara té files a Contactes: el raw contact de la nostra compte que les porta. */
data class ContactRows(
  val rawContactId: Long,
  val personId: Long,
)

/** Contacte del dispositiu resolt pel vincle; l'id i el lookup key poden haver canviat des que es va desar. */
data class ResolvedContact(
  val id: Long,
  val lookupKey: String,
  val name: String,
)

/**
 * L'únic lloc que toca el proveïdor de contactes (`docs/contacts`, C5). Només s'hi escriu el text fix de les files i
 * l'id intern de la persona, mai nom, notes ni comptadors (C3). Cal `READ_CONTACTS` i `WRITE_CONTACTS`: sense
 * permisos, els mètodes que hi accedeixen llancen `SecurityException` (excepte [personIdForRow]).
 */
interface ContactsGateway {
  /** Persones que ara tenen files. */
  suspend fun rows(): List<ContactRows>

  /**
   * Crea (o refà) les files de [personId] enganxades al contacte. Torna el contacte amb el lookup key nou (canvia en
   * agregar), o `null` si el contacte ja no existeix.
   */
  suspend fun addRows(
    personId: Long,
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact?

  suspend fun removeRows(personId: Long)

  /** El contacte del vincle, o `null` si s'ha esborrat. */
  suspend fun resolve(
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact?

  /** El contacte que torna `PickContact` (el sistema en dona accés temporal sense cap permís), o `null` si no es llegeix. */
  suspend fun readPicked(contactUri: String): ResolvedContact?

  /** `personId` de la fila tocada a Contactes (el `content://com.android.contacts/data/N` de l'intent). */
  suspend fun personIdForRow(dataUri: String): Long?

  fun hasAccount(): Boolean

  fun ensureAccount()

  fun removeAccount()
}

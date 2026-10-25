package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.contacts.ContactRows
import com.alorma.contactnotes.data.contacts.ContactsGateway
import com.alorma.contactnotes.data.contacts.ResolvedContact

/**
 * [ContactsGateway] en memòria. [contacts] són els contactes del dispositiu (per lookup key); afegir files canvia el
 * lookup key com fa l'agregació real ([aggregatedSuffix]). [rowUris] simula les files tocades a Contactes.
 */
class FakeContactsGateway : ContactsGateway {
  val contacts = mutableMapOf<String, ResolvedContact>()
  val rowUris = mutableMapOf<String, Long>()
  private val rowsByPerson = linkedMapOf<Long, ContactRows>()
  private var nextRawId = 1L
  var accountExists = false
  var aggregatedSuffix = ".agg"

  fun addContact(contact: ResolvedContact) {
    contacts[contact.lookupKey] = contact
  }

  /** Fusiona el contacte [from] dins [into]: els lookup keys vells continuen resolent, ara amb l'id de [into]. */
  fun mergeContacts(
    from: Long,
    into: Long,
  ) {
    contacts.replaceAll { _, contact -> if (contact.id == from) contact.copy(id = into) else contact }
    rowsByPerson.replaceAll { _, rows -> if (rows.contactId == from) rows.copy(contactId = into) else rows }
  }

  /** Esborra el contacte del dispositiu: les nostres files hi queden soles, com fa el proveïdor real. */
  fun deleteContact(contactId: Long) {
    contacts.values.removeAll { it.id == contactId }
    rowsByPerson.replaceAll { _, rows -> if (rows.contactId == contactId) rows.copy(alone = true) else rows }
  }

  /** El text de les files en l'idioma del moment; canviar-lo simula un canvi d'idioma. */
  var labels = listOf("View notes", "Create note")

  /** Llançat per [rows] i [addRows] quan no hi ha permís. */
  var denied = false

  override suspend fun rows(): List<ContactRows> {
    if (denied) throw SecurityException()
    return rowsByPerson.values.toList()
  }

  override fun rowLabels(): List<String> = labels

  override suspend fun addRows(
    personId: Long,
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact? {
    if (denied) throw SecurityException()
    val contact = contacts[lookupKey] ?: contacts.values.firstOrNull { it.id == contactId } ?: return null
    rowsByPerson[personId] =
      ContactRows(rawContactId = nextRawId++, personId = personId, contactId = contact.id, labels = labels)
    val aggregated = contact.copy(lookupKey = contact.lookupKey + aggregatedSuffix)
    contacts.remove(contact.lookupKey)
    contacts[aggregated.lookupKey] = aggregated
    return aggregated
  }

  override suspend fun removeRows(personId: Long) {
    rowsByPerson.remove(personId)
  }

  override suspend fun resolve(
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact? = contacts[lookupKey] ?: contacts.values.firstOrNull { it.id == contactId }

  /** Contactes que torna el selector, per URI. */
  val pickable = mutableMapOf<String, ResolvedContact>()

  override suspend fun readPicked(contactUri: String): ResolvedContact? = pickable[contactUri]

  override suspend fun personIdForRow(dataUri: String): Long? = rowUris[dataUri]

  override fun hasAccount(): Boolean = accountExists

  override fun ensureAccount() {
    accountExists = true
  }

  override fun removeAccount() {
    accountExists = false
  }
}

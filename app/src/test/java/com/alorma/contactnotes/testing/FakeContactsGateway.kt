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

  override suspend fun rows(): List<ContactRows> = rowsByPerson.values.toList()

  override suspend fun addRows(
    personId: Long,
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact? {
    val contact = contacts[lookupKey] ?: contacts.values.firstOrNull { it.id == contactId } ?: return null
    rowsByPerson[personId] = ContactRows(rawContactId = nextRawId++, personId = personId)
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

  override suspend fun personIdForRow(dataUri: String): Long? = rowUris[dataUri]

  override fun hasAccount(): Boolean = accountExists

  override fun ensureAccount() {
    accountExists = true
  }

  override fun removeAccount() {
    accountExists = false
  }
}

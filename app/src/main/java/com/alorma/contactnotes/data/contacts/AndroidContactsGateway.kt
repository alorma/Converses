package com.alorma.contactnotes.data.contacts

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.AggregationExceptions
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.core.net.toUri
import com.alorma.contactnotes.R
import com.alorma.contactnotes.ui.navigation.ContactRowAction
import com.alorma.contactnotes.ui.navigation.contactRowMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [ContactsGateway] sobre `ContactsContract`, com a sync adapter de la compte «Converses». Cada persona vinculada té un
 * raw contact sense nom (l'id de la persona a `SYNC1`) amb una fila per [ContactRowAction] i una
 * `AggregationExceptions` que l'enganxa al contacte triat.
 */
class AndroidContactsGateway(
  private val context: Context,
) : ContactsGateway {
  private val resolver get() = context.contentResolver
  private val accountType get() = context.getString(R.string.contacts_account_type)
  private val account get() = Account(ACCOUNT_NAME, accountType)

  override suspend fun rows(): List<ContactRows> =
    withContext(Dispatchers.IO) {
      resolver
        .query(
          RawContacts.CONTENT_URI,
          arrayOf(RawContacts._ID, RawContacts.SYNC1),
          "${RawContacts.ACCOUNT_TYPE} = ? AND ${RawContacts.DELETED} = 0",
          arrayOf(accountType),
          null,
        )?.use { cursor ->
          buildList {
            while (cursor.moveToNext()) {
              val personId = cursor.getString(1)?.toLongOrNull() ?: continue
              add(ContactRows(rawContactId = cursor.getLong(0), personId = personId))
            }
          }
        }.orEmpty()
    }

  override suspend fun addRows(
    personId: Long,
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact? =
    withContext(Dispatchers.IO) {
      val contact = resolveNow(contactId, lookupKey) ?: return@withContext null
      val target = foreignRawContactOf(contact.id) ?: return@withContext null
      removeRowsNow(personId)

      val ops =
        arrayListOf(
          ContentProviderOperation
            .newInsert(RawContacts.CONTENT_URI.asSyncAdapter())
            .withValue(RawContacts.ACCOUNT_TYPE, accountType)
            .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
            .withValue(RawContacts.SYNC1, personId.toString())
            .build(),
        )
      ContactRowAction.entries.forEach { action ->
        ops +=
          ContentProviderOperation
            .newInsert(Data.CONTENT_URI.asSyncAdapter())
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, contactRowMimeType(context.packageName, action))
            .withValue(Data.DATA1, personId.toString())
            .withValue(Data.DATA2, context.getString(R.string.app_name))
            .withValue(Data.DATA3, context.getString(action.label))
            .build()
      }
      ops +=
        ContentProviderOperation
          .newUpdate(AggregationExceptions.CONTENT_URI)
          .withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER)
          .withValue(AggregationExceptions.RAW_CONTACT_ID1, target)
          .withValueBackReference(AggregationExceptions.RAW_CONTACT_ID2, 0)
          .build()
      resolver.applyBatch(ContactsContract.AUTHORITY, ops)

      // Agregar canvia el lookup key del contacte: es torna el nou perquè es desi
      resolveNow(contact.id, contact.lookupKey) ?: contact
    }

  override suspend fun removeRows(personId: Long) = withContext(Dispatchers.IO) { removeRowsNow(personId) }

  override suspend fun resolve(
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact? = withContext(Dispatchers.IO) { resolveNow(contactId, lookupKey) }

  override suspend fun personIdForRow(dataUri: String): Long? =
    withContext(Dispatchers.IO) {
      // Sense permís (o una URI que no és de Contactes) es tracta com una fila que no es pot llegir
      runCatching {
        resolver.query(dataUri.toUri(), arrayOf(Data.DATA1), null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) cursor.getString(0)?.toLongOrNull() else null
        }
      }.getOrNull()
    }

  override fun hasAccount(): Boolean = AccountManager.get(context).getAccountsByType(accountType).isNotEmpty()

  override fun ensureAccount() {
    if (!hasAccount()) AccountManager.get(context).addAccountExplicitly(account, null, null)
  }

  override fun removeAccount() {
    if (hasAccount()) AccountManager.get(context).removeAccountExplicitly(account)
  }

  private fun removeRowsNow(personId: Long) {
    resolver.delete(
      RawContacts.CONTENT_URI.asSyncAdapter(),
      "${RawContacts.ACCOUNT_TYPE} = ? AND ${RawContacts.SYNC1} = ?",
      arrayOf(accountType, personId.toString()),
    )
  }

  private fun resolveNow(
    contactId: Long,
    lookupKey: String,
  ): ResolvedContact? {
    val uri = Contacts.lookupContact(resolver, Contacts.getLookupUri(contactId, lookupKey)) ?: return null
    return resolver
      .query(uri, arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          ResolvedContact(id = cursor.getLong(0), lookupKey = cursor.getString(1), name = cursor.getString(2).orEmpty())
        } else {
          null
        }
      }
  }

  /** Un raw contact del contacte que no sigui nostre, per enganxar-hi el de la persona. */
  private fun foreignRawContactOf(contactId: Long): Long? =
    resolver
      .query(
        RawContacts.CONTENT_URI,
        arrayOf(RawContacts._ID),
        "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.ACCOUNT_TYPE} IS NOT ? AND ${RawContacts.DELETED} = 0",
        arrayOf(contactId.toString(), accountType),
        null,
      )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

  private fun Uri.asSyncAdapter(): Uri =
    buildUpon()
      .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
      .appendQueryParameter(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
      .appendQueryParameter(RawContacts.ACCOUNT_TYPE, accountType)
      .build()

  private val ContactRowAction.label: Int
    get() =
      when (this) {
        ContactRowAction.ViewNotes -> R.string.contacts_row_view_notes
        ContactRowAction.CreateNote -> R.string.contacts_row_create_note
      }

  private companion object {
    /** Nom de la compte a Ajustos › Comptes; és la marca, no es tradueix. */
    const val ACCOUNT_NAME = "Converses"
  }
}

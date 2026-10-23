package com.alorma.contactnotes.debug.contacts

import android.accounts.AccountManager
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.AggregationExceptions
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.provider.ContactsPickerSessionContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.alorma.contactnotes.MainActivity

// PROVA DE CONCEPTE (fase 0 de docs/contacts): no es fusiona mai a main.
// A=com.alorma.contactnotes.dev/com.alorma.contactnotes.debug.contacts.ContactsPocActivity
// Via antiga (fins a 37.1): adb shell am start -n $A --el personId 2 --es action legacy
// Via nova (37.2+):         adb shell am start -n $A --el personId 2 --es action directory
// Esborrar les files:       adb shell am start -n $A --es action clear

const val POC_TAG = "ContactsPoc"

class ContactsPocActivity : ComponentActivity() {
  private val personId by lazy { intent.getLongExtra("personId", -1) }

  private val pickLegacy =
    registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
      uri?.let { runCatching { linkLegacy(it) }.onFailure { e -> Log.e(POC_TAG, "legacy failed", e) } }
      finish()
    }

  private val pickDirectory =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val session = result.data?.data
      Log.i(POC_TAG, "picker result=${result.resultCode} session=$session")
      if (result.resultCode == Activity.RESULT_OK && session != null) {
        runCatching { saveDirectory(session) }.onFailure { e -> Log.e(POC_TAG, "directory failed", e) }
      }
      finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) return
    when (intent.getStringExtra("action")) {
      "legacy" -> pickLegacy.launch(null)
      "directory" ->
        pickDirectory.launch(
          Intent(ContactsPickerSessionContract.ACTION_PICK_CONTACTS).putStringArrayListExtra(
            ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
            arrayListOf(Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE),
          ),
        )
      "clear" -> {
        val deleted =
          contentResolver.delete(
            RawContacts.CONTENT_URI.asSyncAdapter(),
            "${RawContacts.ACCOUNT_TYPE} = ?",
            arrayOf(POC_ACCOUNT_TYPE),
          )
        Log.i(POC_TAG, "cleared $deleted raw contacts")
        finish()
      }
      else -> finish()
    }
  }

  /** Via nova: el picker dona telèfons i correus del contacte triat sense cap permís. */
  private fun saveDirectory(session: Uri) {
    contentResolver.query(session, null, null, null, null)!!.use { cursor ->
      Log.i(POC_TAG, "session columns=${cursor.columnNames.toList()} rows=${cursor.count}")
      val name = cursor.getColumnIndex(Data.DISPLAY_NAME)
      val mime = cursor.getColumnIndex(Data.MIMETYPE)
      val data1 = cursor.getColumnIndex(Data.DATA1)
      val lookup = cursor.getColumnIndex(Data.LOOKUP_KEY)
      var displayName = ""
      val values = mutableListOf<String>()
      while (cursor.moveToNext()) {
        displayName = cursor.getString(name)
        Log.i(
          POC_TAG,
          "row mime=${cursor.getString(mime)} data1=${cursor.getString(data1)} lookup=${cursor.getString(lookup)}",
        )
        values += cursor.getString(data1)
      }
      PocDirectoryStore.save(this, personId, displayName, values)
      Log.i(POC_TAG, "directory saved personId=$personId name=$displayName values=$values")
    }
  }

  /** Via antiga: un RawContact de la nostra compte, amb una sola fila del mimetype propi, enganxat al contacte. */
  private fun linkLegacy(contactUri: Uri) {
    val (contactId, lookupBefore, name) =
      contentResolver
        .query(contactUri, arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME), null, null, null)!!
        .use {
          it.moveToFirst()
          Triple(it.getLong(0), it.getString(1), it.getString(2))
        }
    Log.i(POC_TAG, "picked id=$contactId lookup=$lookupBefore name=$name personId=$personId")

    Log.i(POC_TAG, "account added=${AccountManager.get(this).addAccountExplicitly(pocAccount, null, null)}")

    val targetRaw =
      contentResolver
        .query(
          RawContacts.CONTENT_URI,
          arrayOf(RawContacts._ID),
          "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.ACCOUNT_TYPE} IS NOT ?",
          arrayOf(contactId.toString(), POC_ACCOUNT_TYPE),
          null,
        )!!
        .use {
          it.moveToFirst()
          it.getLong(0)
        }

    val ops =
      arrayListOf(
        ContentProviderOperation
          .newInsert(RawContacts.CONTENT_URI.asSyncAdapter())
          .withValue(RawContacts.ACCOUNT_TYPE, POC_ACCOUNT_TYPE)
          .withValue(RawContacts.ACCOUNT_NAME, POC_ACCOUNT_NAME)
          .withValue(RawContacts.SYNC1, personId.toString())
          .build(),
        ContentProviderOperation
          .newInsert(Data.CONTENT_URI.asSyncAdapter())
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, POC_MIMETYPE)
          .withValue(Data.DATA1, personId.toString())
          .withValue(Data.DATA2, "Converses")
          .withValue(Data.DATA3, "Veure notes")
          .build(),
        ContentProviderOperation
          .newUpdate(AggregationExceptions.CONTENT_URI)
          .withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER)
          .withValue(AggregationExceptions.RAW_CONTACT_ID1, targetRaw)
          .withValueBackReference(AggregationExceptions.RAW_CONTACT_ID2, 0)
          .build(),
      )
    val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    Log.i(POC_TAG, "inserted raw=${results[0].uri} data=${results[1].uri}")

    contentResolver
      .query(
        Contacts.CONTENT_URI,
        arrayOf(Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME),
        "${Contacts._ID} = ?",
        arrayOf("$contactId"),
        null,
      )?.use { if (it.moveToFirst()) Log.i(POC_TAG, "after lookup=${it.getString(0)} name=${it.getString(1)}") }
    val resolvedOld = Contacts.lookupContact(contentResolver, Contacts.getLookupUri(contactId, lookupBefore))
    Log.i(POC_TAG, "old lookup key resolves to $resolvedOld")
  }
}

/** Via antiga: Contactes obre aquesta activitat en tocar la fila «Converses». */
class ContactsPocViewActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val personId =
      intent.data?.let { uri ->
        contentResolver.query(uri, arrayOf(Data.DATA1), null, null, null)?.use {
          if (it.moveToFirst()) it.getString(0) else null
        }
      }
    Log.i(POC_TAG, "view action=${intent.action} type=${intent.type} uri=${intent.data} personId=$personId")
    openApp(this)
    finish()
  }
}

/** Via nova: Contactes o Telèfon llancen la interacció amb una persona del directori. */
class ContactsPocInteractionActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val type = intent.getStringExtra("android.provider.extra.PEOPLE_INTERACTION_TYPE")
    val args = intent.getBundleExtra("android.provider.extra.PEOPLE_QUERY_ARGS")
    Log.i(POC_TAG, "interaction action=${intent.action} type=$type args=${args?.dump()}")
    openApp(this)
    finish()
  }
}

private fun openApp(context: Context) =
  context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

private fun Uri.asSyncAdapter(): Uri =
  buildUpon()
    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
    .appendQueryParameter(RawContacts.ACCOUNT_NAME, POC_ACCOUNT_NAME)
    .appendQueryParameter(RawContacts.ACCOUNT_TYPE, POC_ACCOUNT_TYPE)
    .build()

@Suppress("DEPRECATION")
fun Bundle.dump(): String =
  keySet().joinToString(prefix = "{", postfix = "}") { key ->
    when (val value = get(key)) {
      is Bundle -> "$key=${value.dump()}"
      is ArrayList<*> -> "$key=${value.map { (it as? Bundle)?.dump() ?: it }}"
      is Array<*> -> "$key=${value.toList()}"
      else -> "$key=$value"
    }
  }

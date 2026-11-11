package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.Tracker
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * El document `users/{uid}` de Firestore sobre la instància `sync`. D'aquesta fase només en surten `wrappedKey`,
 * `format` i `createdAt`; la resta de camps (`plan`, `devices`…) arriben amb les fases 4 i 5.
 */
class FirestoreAccountRemote(
  private val firebase: SyncFirebase,
  private val tracker: Tracker,
) : AccountRemote {
  override suspend fun load(uid: String): RemoteResult<RemoteAccount?> =
    try {
      // Del servidor: la memòria cau podria dir que el compte no té clau quan un altre dispositiu ja l'ha creat
      val snapshot = document(uid).get(Source.SERVER).awaitTask()
      if (snapshot.exists()) {
        val wrappedKey = (snapshot.get(FIELD_WRAPPED_KEY) as? Blob)?.toBytes()
        val format = (snapshot.get(FIELD_FORMAT) as? Number)?.toInt()
        check(wrappedKey != null && wrappedKey.isNotEmpty() && format != null) { "Account without a valid key" }
        RemoteResult.Ok(RemoteAccount(wrappedKey, format))
      } else {
        RemoteResult.Ok(null)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e.isNetworkFailure()) {
        RemoteResult.Offline
      } else {
        tracker.trackError(e, ErrorSites.SYNC_ACCOUNT)
        RemoteResult.Failed
      }
    }

  override suspend fun create(
    uid: String,
    account: RemoteAccount,
  ): CreateResult =
    try {
      val document = document(uid)
      val data =
        mapOf(
          FIELD_WRAPPED_KEY to Blob.fromBytes(account.wrappedKey),
          FIELD_FORMAT to account.format,
          FIELD_CREATED_AT to FieldValue.serverTimestamp(),
        )
      // La lectura dins la transacció fa que l'escriptura només valgui si el document continua sense existir: de dos
      // dispositius que s'activen alhora, el segon torna a llegir, el troba i no trepitja la clau del primer
      val created =
        firebase.firestore
          .runTransaction { transaction ->
            if (transaction.get(document).exists()) {
              false
            } else {
              transaction.set(document, data)
              true
            }
          }.awaitTask()
      if (created) CreateResult.Created else CreateResult.AlreadyExists
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e.isNetworkFailure()) {
        CreateResult.Offline
      } else {
        tracker.trackError(e, ErrorSites.SYNC_ACCOUNT)
        CreateResult.Failed
      }
    }

  /** Crear la instància llegeix del disc. */
  private suspend fun document(uid: String): DocumentReference =
    withContext(Dispatchers.IO) { firebase.firestore.collection(COLLECTION_USERS).document(uid) }

  private companion object {
    const val COLLECTION_USERS = "users"
    const val FIELD_WRAPPED_KEY = "wrappedKey"
    const val FIELD_FORMAT = "format"
    const val FIELD_CREATED_AT = "createdAt"
  }
}

package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.Tracker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Els registres xifrats a `users/{uid}/records/{id}` de Firestore, sobre la instància `sync`. Les regles
 * (`firestore.rules`) en fixen la forma: `c`, `deleted`, `by` i `updatedAt`, que posa el servidor.
 *
 * Mai es passa cap missatge d'error a l'analítica: només l'excepció i el lloc.
 */
class FirestoreSyncRemote(
  private val firebase: SyncFirebase,
  private val tracker: Tracker,
) : SyncRemote {
  override suspend fun push(
    uid: String,
    records: List<RemoteRecord>,
  ): RemoteResult<Unit> =
    guarded {
      if (records.isNotEmpty()) {
        val collection = recordsOf(uid)
        val batch = withContext(Dispatchers.IO) { firebase.firestore.batch() }
        records.forEach { record ->
          batch.set(
            collection.document(record.id),
            mapOf(
              FIELD_CONTENT to Blob.fromBytes(record.sealed),
              FIELD_DELETED to record.deleted,
              FIELD_BY to record.by,
              FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().awaitTask()
      }
      Unit
    }

  override suspend fun pull(
    uid: String,
    since: Long,
    limit: Int,
  ): RemoteResult<List<RemoteRecord>> =
    guarded {
      // Del servidor: la memòria cau no coneix el que han escrit els altres dispositius
      afterSince(recordsOf(uid), since)
        .limit(limit.toLong())
        .get(Source.SERVER)
        .awaitTask()
        .documents
        .mapNotNull { it.toRemoteRecord() }
    }

  override fun listen(
    uid: String,
    since: Long,
  ): Flow<List<RemoteRecord>> =
    callbackFlow {
      val registration =
        afterSince(recordsOf(uid), since).addSnapshotListener { snapshot, error ->
          if (error != null) {
            if (!error.isNetworkFailure()) tracker.trackError(error, ErrorSites.SYNC_ACCOUNT)
            // El motor torna a obrir l'escolta quan tocarà
            close()
          } else if (snapshot != null) {
            // Només el que ha canviat, i no els nostres propis registres encara sense hora del servidor
            val changed =
              snapshot.documentChanges
                .filter { it.type != DocumentChange.Type.REMOVED && !it.document.metadata.hasPendingWrites() }
                .mapNotNull { it.document.toRemoteRecord() }
            if (changed.isNotEmpty()) trySend(changed)
          }
        }
      awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

  override suspend fun registerDevice(
    uid: String,
    device: RemoteDevice,
  ): RemoteResult<Unit> =
    guarded {
      val account = withContext(Dispatchers.IO) { firebase.firestore.collection(COLLECTION_USERS).document(uid) }
      val plan = if (device.pro) PLAN_PRO else PLAN_FREE
      // La lectura dins la transacció deixa `addedAt` com era: només es posa la primera vegada
      firebase.firestore
        .runTransaction { transaction ->
          val known = (transaction.get(account).get(FIELD_DEVICES) as? Map<*, *>)?.containsKey(device.id) == true
          val name = Blob.fromBytes(device.sealedName)
          if (known) {
            transaction.update(
              account,
              FieldPath.of(FIELD_DEVICES, device.id, FIELD_DEVICE_NAME),
              name,
              FieldPath.of(FIELD_DEVICES, device.id, FIELD_LAST_SYNC_AT),
              FieldValue.serverTimestamp(),
              FieldPath.of(FIELD_PLAN),
              plan,
              FieldPath.of(FIELD_LAST_SYNC_AT),
              FieldValue.serverTimestamp(),
            )
          } else {
            transaction.update(
              account,
              FieldPath.of(FIELD_DEVICES, device.id),
              mapOf(
                FIELD_DEVICE_NAME to name,
                FIELD_ADDED_AT to FieldValue.serverTimestamp(),
                FIELD_LAST_SYNC_AT to FieldValue.serverTimestamp(),
              ),
              FieldPath.of(FIELD_PLAN),
              plan,
              FieldPath.of(FIELD_LAST_SYNC_AT),
              FieldValue.serverTimestamp(),
            )
          }
        }.awaitTask()
      Unit
    }

  private suspend fun <T> guarded(block: suspend () -> T): RemoteResult<T> =
    try {
      RemoteResult.Ok(block())
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

  /** Crear la instància llegeix del disc. */
  private suspend fun recordsOf(uid: String): CollectionReference =
    withContext(Dispatchers.IO) {
      firebase.firestore
        .collection(COLLECTION_USERS)
        .document(uid)
        .collection(COLLECTION_RECORDS)
    }

  private fun afterSince(
    collection: CollectionReference,
    since: Long,
  ): Query = collection.whereGreaterThan(FIELD_UPDATED_AT, since.toTimestamp()).orderBy(FIELD_UPDATED_AT)

  /** `null` si el document no té la forma que les regles exigeixen: no hauria de passar, i no es pot fer res amb ell. */
  private fun DocumentSnapshot.toRemoteRecord(): RemoteRecord? {
    val sealed = (get(FIELD_CONTENT) as? Blob)?.toBytes()
    val deleted = getBoolean(FIELD_DELETED)
    val by = getString(FIELD_BY)
    val updatedAt = getTimestamp(FIELD_UPDATED_AT)
    return if (sealed != null && deleted != null && by != null && updatedAt != null) {
      RemoteRecord(id, sealed, deleted, by, updatedAt.toMillis())
    } else {
      null
    }
  }

  private fun Timestamp.toMillis(): Long = seconds * MILLIS_PER_SECOND + nanoseconds / NANOS_PER_MILLI

  private fun Long.toTimestamp(): Timestamp =
    Timestamp(
      Math.floorDiv(this, MILLIS_PER_SECOND),
      Math.floorMod(this, MILLIS_PER_SECOND).toInt() * NANOS_PER_MILLI,
    )

  private companion object {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_RECORDS = "records"
    const val FIELD_CONTENT = "c"
    const val FIELD_DELETED = "deleted"
    const val FIELD_BY = "by"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_DEVICES = "devices"
    const val FIELD_DEVICE_NAME = "name"
    const val FIELD_ADDED_AT = "addedAt"
    const val FIELD_LAST_SYNC_AT = "lastSyncAt"
    const val FIELD_PLAN = "plan"
    const val PLAN_PRO = "pro"
    const val PLAN_FREE = "free"
    const val MILLIS_PER_SECOND = 1000L
    const val NANOS_PER_MILLI = 1_000_000
  }
}

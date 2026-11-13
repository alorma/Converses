package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.Tracker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
  ): RemoteResult<Unit> {
    if (records.isEmpty()) return RemoteResult.Ok(Unit)
    // Sense xarxa, Firestore no fa fallar l'escriptura: la deixa a la cua i espera la connexió. Sense límit, el motor
    // es quedaria «Sincronitzant» per sempre i amb el pany agafat. Si el servidor no confirma a temps es diu «sense
    // connexió»; el lot es queda a la cua de Firestore i tornar-lo a pujar no fa res, perquè es reescriu igual
    return withTimeoutOrNull(PUSH_TIMEOUT_MILLIS) { commit(uid, records) } ?: RemoteResult.Offline
  }

  private suspend fun commit(
    uid: String,
    records: List<RemoteRecord>,
  ): RemoteResult<Unit> =
    guarded {
      run {
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

  override suspend fun loadDevices(uid: String): RemoteResult<RemoteDevices> =
    guarded {
      // Del servidor: la memòria cau no sap què han fet els altres dispositius
      accountOf(uid).get(Source.SERVER).awaitTask().toRemoteDevices()
    }

  override fun observeDevices(uid: String): Flow<RemoteDevices> =
    callbackFlow {
      val registration =
        accountOf(uid).addSnapshotListener { snapshot, error ->
          if (error != null) {
            if (!error.isNetworkFailure()) tracker.trackError(error, ErrorSites.SYNC_ACCOUNT)
            // Qui escolta torna a obrir-lo quan toqui
            close()
          } else if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
            trySend(snapshot.toRemoteDevices())
          }
        }
      awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

  override suspend fun joinDevice(
    uid: String,
    device: RemoteDevice,
    removing: String?,
  ): JoinResult =
    try {
      val account = accountOf(uid)
      val plan = if (device.pro) PLAN_PRO else PLAN_FREE
      // La lectura dins la transacció deixa `addedAt` com era i no trepitja el nom que ja hi ha (l'usuari el pot haver
      // canviat des d'un altre dispositiu): només es posa la primera vegada
      firebase.firestore
        .runTransaction { transaction ->
          val known = (transaction.get(account).get(FIELD_DEVICES) as? Map<*, *>)?.containsKey(device.id) == true
          val fields = mutableListOf<Pair<FieldPath, Any?>>()
          if (removing != null && removing != device.id) {
            fields += FieldPath.of(FIELD_DEVICES, removing) to FieldValue.delete()
          }
          if (known) {
            fields += FieldPath.of(FIELD_DEVICES, device.id, FIELD_LAST_SYNC_AT) to FieldValue.serverTimestamp()
          } else {
            fields +=
              FieldPath.of(FIELD_DEVICES, device.id) to
              mapOf(
                FIELD_DEVICE_NAME to Blob.fromBytes(device.sealedName),
                FIELD_ADDED_AT to FieldValue.serverTimestamp(),
                FIELD_LAST_SYNC_AT to FieldValue.serverTimestamp(),
              )
          }
          fields += FieldPath.of(FIELD_PLAN) to plan
          fields += FieldPath.of(FIELD_LAST_SYNC_AT) to FieldValue.serverTimestamp()
          transaction.updateFields(account, fields)
        }.awaitTask()
      JoinResult.Joined
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      when {
        e.isNetworkFailure() -> JoinResult.Offline
        // Les regles no deixen passar de les places del pla
        e.isPermissionDenied() -> JoinResult.Full
        else -> {
          tracker.trackError(e, ErrorSites.SYNC_ACCOUNT)
          JoinResult.Failed
        }
      }
    }

  override suspend fun removeDevice(
    uid: String,
    id: String,
  ): RemoteResult<Unit> = updateAccount(uid, FieldPath.of(FIELD_DEVICES, id) to FieldValue.delete())

  override suspend fun renameDevice(
    uid: String,
    id: String,
    sealedName: ByteArray,
  ): RemoteResult<Unit> =
    updateIfKnown(uid, id, FieldPath.of(FIELD_DEVICES, id, FIELD_DEVICE_NAME) to Blob.fromBytes(sealedName))

  override suspend fun touchDevice(
    uid: String,
    id: String,
  ): RemoteResult<Unit> =
    updateIfKnown(
      uid,
      id,
      FieldPath.of(FIELD_DEVICES, id, FIELD_LAST_SYNC_AT) to FieldValue.serverTimestamp(),
      FieldPath.of(FIELD_LAST_SYNC_AT) to FieldValue.serverTimestamp(),
    )

  /**
   * Un `update` amb camins niats crea el mapa que falta: sobre un dispositiu ja tret el tornaria a apuntar, amb una entrada
   * a mitges i fora de la protecció de `registered`. Per això es mira dins la transacció que l'entrada encara hi és; si no hi
   * és no es fa res (qui escolta `devices` ja veu que ha estat tret).
   */
  private suspend fun updateIfKnown(
    uid: String,
    id: String,
    vararg fields: Pair<FieldPath, Any?>,
  ): RemoteResult<Unit> =
    guarded {
      val account = accountOf(uid)
      firebase.firestore
        .runTransaction { transaction ->
          val known = (transaction.get(account).get(FIELD_DEVICES) as? Map<*, *>)?.containsKey(id) == true
          if (known) transaction.updateFields(account, fields.toList())
        }.awaitTask()
      Unit
    }

  override suspend fun setPlan(
    uid: String,
    pro: Boolean,
  ): RemoteResult<Unit> = updateAccount(uid, FieldPath.of(FIELD_PLAN) to if (pro) PLAN_PRO else PLAN_FREE)

  private suspend fun updateAccount(
    uid: String,
    vararg fields: Pair<FieldPath, Any?>,
  ): RemoteResult<Unit> =
    guarded {
      val (first, rest) = fields.first() to fields.drop(1)
      val args = rest.flatMap { listOf(it.first, it.second) }.toTypedArray()
      accountOf(uid).update(first.first, first.second, *args).awaitTask()
      Unit
    }

  private fun Transaction.updateFields(
    account: DocumentReference,
    fields: List<Pair<FieldPath, Any?>>,
  ) {
    val args = fields.drop(1).flatMap { listOf(it.first, it.second) }.toTypedArray()
    update(account, fields.first().first, fields.first().second, *args)
  }

  private suspend fun accountOf(uid: String): DocumentReference =
    withContext(Dispatchers.IO) { firebase.firestore.collection(COLLECTION_USERS).document(uid) }

  /** Sense el camp `devices` (o amb una forma que no s'espera) és un compte sense dispositius. */
  private fun DocumentSnapshot.toRemoteDevices(): RemoteDevices {
    val devices = get(FIELD_DEVICES) as? Map<*, *> ?: emptyMap<Any, Any>()
    val entries =
      devices.mapNotNull { (key, value) ->
        val id = key as? String ?: return@mapNotNull null
        val fields = value as? Map<*, *> ?: return@mapNotNull null
        RemoteDeviceEntry(
          id = id,
          sealedName = (fields[FIELD_DEVICE_NAME] as? Blob)?.toBytes(),
          addedAt = (fields[FIELD_ADDED_AT] as? Timestamp)?.toMillis(),
          lastSyncAt = (fields[FIELD_LAST_SYNC_AT] as? Timestamp)?.toMillis(),
        )
      }
    return RemoteDevices(pro = getString(FIELD_PLAN) == PLAN_PRO, entries = entries)
  }

  private suspend fun <T> guarded(block: suspend () -> T): RemoteResult<T> =
    try {
      RemoteResult.Ok(block())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e.isNetworkFailure()) {
        RemoteResult.Offline
      } else if (e.isPermissionDenied()) {
        RemoteResult.Denied
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
    const val PUSH_TIMEOUT_MILLIS = 20_000L
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

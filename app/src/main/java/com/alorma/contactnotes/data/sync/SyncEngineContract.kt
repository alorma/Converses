package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.engine.RecordPuller
import com.alorma.contactnotes.data.sync.engine.RecordPusher
import com.alorma.contactnotes.data.sync.record.SyncRecord
import kotlinx.coroutines.flow.Flow

/** Un registre tal com és a Firestore: el contingut va xifrat a [sealed]; la resta és el que les regles necessiten. */
class RemoteRecord(
  /** El `syncId` de la fila. */
  val id: String,
  val sealed: ByteArray,
  val deleted: Boolean,
  /** El `deviceId` que l'ha escrit. */
  val by: String,
  /** Hora del servidor; ordena la baixada. */
  val updatedAt: Long,
) {
  override fun toString(): String = "RemoteRecord"
}

/** Aquest dispositiu a `devices`: el nom va xifrat. */
class RemoteDevice(
  val id: String,
  val sealedName: ByteArray,
  val pro: Boolean,
) {
  override fun toString(): String = "RemoteDevice"
}

/** Els registres d'un compte a Firestore. */
interface SyncRemote {
  /** Escriu els registres; el servidor en posa `updatedAt`. */
  suspend fun push(
    uid: String,
    records: List<RemoteRecord>,
  ): RemoteResult<Unit>

  /** Del servidor, ordenats per `updatedAt`, els posteriors a [since]. */
  suspend fun pull(
    uid: String,
    since: Long,
    limit: Int,
  ): RemoteResult<List<RemoteRecord>>

  /** Canvis nous mentre algú escolta (l'app és visible). */
  fun listen(
    uid: String,
    since: Long,
  ): Flow<List<RemoteRecord>>

  suspend fun registerDevice(
    uid: String,
    device: RemoteDevice,
  ): RemoteResult<Unit>
}

/** El que el motor recorda entre baixades, al DataStore `sync`. */
interface SyncCursorStore {
  /** `updatedAt` del darrer registre baixat; 0 si encara no s'ha baixat res. */
  suspend fun cursor(): Long

  suspend fun setCursor(value: Long)

  /** Aleatori i estable; es crea la primera vegada que es demana. */
  suspend fun deviceId(): String
}

enum class RecordType { Person, Entry, Task, Channel }

/** Una fila pendent de pujar, ja convertida: les referències van per `syncId`. */
class PendingRecord(
  val syncId: String,
  val type: RecordType,
  val record: SyncRecord,
) {
  override fun toString(): String = "PendingRecord"
}

class PendingDeletion(
  val syncId: String,
  val deletedAt: Long,
) {
  override fun toString(): String = "PendingDeletion"
}

class PendingChanges(
  val records: List<PendingRecord>,
  val deletions: List<PendingDeletion>,
) {
  val isEmpty: Boolean get() = records.isEmpty() && deletions.isEmpty()

  override fun toString(): String = "PendingChanges"
}

/** Què hi ha ara al dispositiu per a un `syncId`, per decidir un conflicte. */
sealed interface LocalState {
  data object Absent : LocalState

  data class Live(
    val type: RecordType,
    val modifiedAt: Long,
    val dirty: Boolean,
  ) : LocalState

  /** Esborrada aquí i encara no pujada (o pujada i pendent de netejar). */
  data class Deleted(
    val deletedAt: Long,
  ) : LocalState
}

sealed interface ApplyResult {
  data object Applied : ApplyResult

  /** La persona o el canal propi al qual apunta encara no hi és: s'ha d'esperar. */
  data object MissingReference : ApplyResult

  /** Un valor que aquesta versió no coneix (un enum nou, per exemple): no s'aplica. */
  data object Unreadable : ApplyResult

  /** Un canal propi amb el mateix grup i nom ja existeix aquí amb un altre `syncId`; no s'ha escrit res. */
  data class DuplicateChannel(
    val localSyncId: String,
  ) : ApplyResult
}

/** Un canal propi, per detectar duplicats. */
class ChannelInfo(
  val syncId: String,
  val channelType: String,
  val name: String,
) {
  override fun toString(): String = "ChannelInfo"
}

/**
 * El costat de Room del motor. Tot el que escriu **no segella** (ni `dirty` ni data): un canvi baixat no ha de tornar
 * a pujar. No passa pel `ConversesRepository`.
 */
interface LocalSyncSource {
  suspend fun readPending(): PendingChanges

  /** Treu el `dirty` només si la fila continua amb aquest [modifiedAt]; si l'han editat mentrestant, es queda pendent. */
  suspend fun markClean(
    syncId: String,
    type: RecordType,
    modifiedAt: Long,
  )

  /**
   * Torna a deixar la fila pendent sense tocar-ne la data: el núvol té una versió que perd contra la d'aquí (una pujada
   * antiga que hi ha caigut a sobre) i la d'aquí hi ha de tornar.
   */
  suspend fun markDirty(
    syncId: String,
    type: RecordType,
  )

  /** Esborra el tombstone pujat, si encara és aquest [deletedAt]. */
  suspend fun markDeletionUploaded(
    syncId: String,
    deletedAt: Long,
  )

  suspend fun localState(syncId: String): LocalState

  /** Desa una fila baixada (per `syncId`, conservant l'id local) i treu el tombstone que tingués. */
  suspend fun applyRemote(
    syncId: String,
    record: SyncRecord,
  ): ApplyResult

  /** Esborra la fila (i el que en depèn) sense deixar tombstone: l'esborrat ve de fora. */
  suspend fun applyRemoteDeletion(
    syncId: String,
    type: RecordType,
  )

  /** Perquè un tombstone que ens arriba d'un registre que no tenim no es torni a pujar; vegeu els canals repetits. */
  suspend fun addTombstone(
    syncId: String,
    deletedAt: Long,
  )

  /** Els canals propis d'aquí, per trobar-hi duplicats en baixar. */
  suspend fun ownChannels(): List<ChannelInfo>

  /** El canal local passa a tenir el `syncId` del remot (el més petit guanya); vegeu `CustomChannelDao.adoptSyncId`. */
  suspend fun adoptChannelSyncId(
    localSyncId: String,
    remoteSyncId: String,
    now: Long,
  )

  /** Quantes entrades hi ha, per al diàleg de fusió: mai noms. */
  suspend fun entryCount(): Int

  fun observePendingCount(): Flow<Int>
}

sealed interface SyncStatus {
  /** [at]: quan va acabar l'última sincronització; `null` si no se sap. */
  data class UpToDate(
    val at: Long?,
  ) : SyncStatus

  /** [progress] de 0 a 1 en una pujada o baixada completa; `null` si no es pot mesurar. */
  data class Syncing(
    val progress: Float?,
  ) : SyncStatus

  data class Offline(
    val pending: Int,
  ) : SyncStatus

  data object Failed : SyncStatus
}

enum class SyncOutcome { Done, Offline, Failed }

/**
 * El motor: JVM pur, sense Android ni Firebase. La lògica és a `engine/`; aquí només hi ha les dues peces públiques.
 * [clock] dóna l'hora local; [onError] rep els registres que no s'han pogut llegir (només l'error i el lloc, mai
 * dades); [pageSize] és el nombre de registres per lot de pujada i per pàgina de baixada.
 */
class SyncEngine(
  private val local: LocalSyncSource,
  private val remote: SyncRemote,
  private val cursors: SyncCursorStore,
  private val clock: () -> Long = System::currentTimeMillis,
  private val onError: (Throwable, String) -> Unit = { _, _ -> },
  private val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
  /**
   * Puja el que és `dirty` i els tombstones, per lots. Abans baixa: Firestore escriu sense mirar què hi ha, i un
   * dispositiu que torna d'estar sense xarxa trepitjaria amb la seva edició antiga una de més nova (o un esborrat). Baixant
   * primer, el conflicte es resol aquí i només puja el que hi guanya.
   */
  suspend fun pushPending(
    uid: String,
    cipher: RecordCipher,
  ): SyncOutcome {
    val pulled = pullAll(uid, cipher)
    if (pulled != SyncOutcome.Done) return pulled
    return RecordPusher(local, remote, cursors, pageSize).push(uid, cipher)
  }

  /** Baixa des del cursor. [onProgress] rep els registres processats i el total, si se sap (`null` si no). */
  suspend fun pullAll(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit = { _, _ -> },
  ): SyncOutcome = RecordPuller(local, remote, cursors, clock, onError, pageSize).pull(uid, cipher, onProgress)

  private companion object {
    const val DEFAULT_PAGE_SIZE = 200
  }
}

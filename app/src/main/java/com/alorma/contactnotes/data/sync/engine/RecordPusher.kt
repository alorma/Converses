package com.alorma.contactnotes.data.sync.engine

import com.alorma.contactnotes.data.sync.LocalSyncSource
import com.alorma.contactnotes.data.sync.RemoteRecord
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SyncCursorStore
import com.alorma.contactnotes.data.sync.SyncOutcome
import com.alorma.contactnotes.data.sync.SyncRemote
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.record.RecordCodec

/**
 * La pujada: converteix els pendents en registres xifrats i els envia per lots. Cada lot que el servidor accepta es
 * marca net just després; si un lot falla, els següents no s'intenten i el que queda continua pendent.
 */
internal class RecordPusher(
  private val local: LocalSyncSource,
  private val remote: SyncRemote,
  private val cursors: SyncCursorStore,
  private val batchSize: Int,
) {
  private class Item(
    val record: RemoteRecord,
    val onUploaded: suspend () -> Unit,
  )

  suspend fun push(
    uid: String,
    cipher: RecordCipher,
  ): SyncOutcome {
    val pending = local.readPending()
    if (pending.isEmpty) return SyncOutcome.Done
    val by = cursors.deviceId()
    // updatedAt és 0: el posa el servidor, mai el client
    val items =
      pending.records.map { p ->
        val sealed = cipher.encrypt(p.syncId, RecordCodec.encode(p.record))
        // Només es treu el `dirty` si la fila continua igual: una edició feta durant la pujada es queda pendent
        Item(RemoteRecord(p.syncId, sealed, deleted = false, by = by, updatedAt = 0)) {
          local.markClean(p.syncId, p.type, p.record.modifiedAt)
        }
      } +
        pending.deletions.map { d ->
          val sealed = cipher.encrypt(d.syncId, RecordCodec.encodeDeleted(d.deletedAt))
          Item(RemoteRecord(d.syncId, sealed, deleted = true, by = by, updatedAt = 0)) {
            local.markDeletionUploaded(d.syncId, d.deletedAt)
          }
        }
    for (batch in items.chunked(batchSize)) {
      when (remote.push(uid, batch.map { it.record })) {
        is RemoteResult.Ok -> batch.forEach { it.onUploaded() }
        RemoteResult.Offline -> return SyncOutcome.Offline
        RemoteResult.Failed -> return SyncOutcome.Failed
      }
    }
    return SyncOutcome.Done
  }
}

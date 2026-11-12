package com.alorma.contactnotes.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Encara no parla amb Firestore (tasca B): no escriu res, no baixa res i no falla mai. */
class FirestoreSyncRemote : SyncRemote {
  override suspend fun push(
    uid: String,
    records: List<RemoteRecord>,
  ): RemoteResult<Unit> = RemoteResult.Ok(Unit)

  override suspend fun pull(
    uid: String,
    since: Long,
    limit: Int,
  ): RemoteResult<List<RemoteRecord>> = RemoteResult.Ok(emptyList())

  override fun listen(
    uid: String,
    since: Long,
  ): Flow<List<RemoteRecord>> = emptyFlow()

  override suspend fun registerDevice(
    uid: String,
    device: RemoteDevice,
  ): RemoteResult<Unit> = RemoteResult.Ok(Unit)
}

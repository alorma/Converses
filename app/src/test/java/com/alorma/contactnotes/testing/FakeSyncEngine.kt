package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.sync.RemoteDevice
import com.alorma.contactnotes.data.sync.RemoteRecord
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SyncCursorStore
import com.alorma.contactnotes.data.sync.SyncRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Firestore en memòria. Posa `updatedAt` creixent a cada escriptura, com el servidor. [failure] fa respondre «sense
 * connexió» o «error» a tot; [shuffled] torna els registres desordenats a [pull] (ho pot fer el servidor amb empats).
 */
class FakeSyncRemote : SyncRemote {
  val records = mutableMapOf<String, RemoteRecord>()
  val devices = mutableListOf<RemoteDevice>()
  var failure: RemoteResult<Nothing>? = null
  var shuffled = false
  var pushes = 0
    private set
  private var clock = 0L
  private val changes = MutableSharedFlow<List<RemoteRecord>>(extraBufferCapacity = 16)

  override suspend fun push(
    uid: String,
    records: List<RemoteRecord>,
  ): RemoteResult<Unit> {
    failure?.let { return it }
    pushes++
    val written = records.map { RemoteRecord(it.id, it.sealed, it.deleted, it.by, ++clock) }
    written.forEach { this.records[it.id] = it }
    changes.tryEmit(written)
    return RemoteResult.Ok(Unit)
  }

  override suspend fun pull(
    uid: String,
    since: Long,
    limit: Int,
  ): RemoteResult<List<RemoteRecord>> {
    failure?.let { return it }
    val page =
      records.values
        .filter { it.updatedAt > since }
        .sortedBy { it.updatedAt }
        .take(limit)
    return RemoteResult.Ok(if (shuffled) page.reversed() else page)
  }

  override fun listen(
    uid: String,
    since: Long,
  ): Flow<List<RemoteRecord>> = changes.asSharedFlow()

  override suspend fun registerDevice(
    uid: String,
    device: RemoteDevice,
  ): RemoteResult<Unit> {
    failure?.let { return it }
    devices.removeAll { it.id == device.id }
    devices += device
    return RemoteResult.Ok(Unit)
  }
}

class FakeCursorStore(
  var cursor: Long = 0L,
  private val deviceId: String = "device-1",
) : SyncCursorStore {
  override suspend fun cursor(): Long = cursor

  override suspend fun setCursor(value: Long) {
    cursor = value
  }

  override suspend fun deviceId(): String = deviceId
}

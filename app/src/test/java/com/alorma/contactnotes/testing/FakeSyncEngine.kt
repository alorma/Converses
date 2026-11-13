package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.sync.DeviceStore
import com.alorma.contactnotes.data.sync.JoinResult
import com.alorma.contactnotes.data.sync.RemoteDevice
import com.alorma.contactnotes.data.sync.RemoteDeviceEntry
import com.alorma.contactnotes.data.sync.RemoteDevices
import com.alorma.contactnotes.data.sync.RemoteRecord
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SyncCursorStore
import com.alorma.contactnotes.data.sync.SyncRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Firestore en memòria. Posa `updatedAt` creixent a cada escriptura, com el servidor. [failure] fa respondre «sense
 * connexió» o «error» a tot; [shuffled] torna els registres desordenats a [pull] (ho pot fer el servidor amb empats).
 */
class FakeSyncRemote : SyncRemote {
  val records = mutableMapOf<String, RemoteRecord>()
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

  private class Entry(
    var sealedName: ByteArray,
    var addedAt: Long,
    var lastSyncAt: Long,
  )

  private val entries = linkedMapOf<String, Entry>()
  private val observed = MutableStateFlow(RemoteDevices(pro = false, entries = emptyList()))

  /** El camp `plan` de `users/{uid}`. */
  var pro = false
    private set

  /** Els dispositius apuntats, tal com els veu el servidor. */
  val devices: List<RemoteDevice> get() = entries.map { (id, entry) -> RemoteDevice(id, entry.sealedName, pro) }

  val deviceIds: List<String> get() = entries.keys.toList()

  fun lastSyncAt(id: String): Long? = entries[id]?.lastSyncAt

  /** Com si un altre dispositiu hagués tret [id] de la llista. */
  fun removeFromOutside(id: String) {
    entries.remove(id)
    publish()
  }

  /**
   * Com la primera instantània d'un listener nou, encara sense [missing]: de la memòria cau o, si [fromCache] és fals,
   * una còpia del servidor d'abans de la transacció. El servidor (`loadDevices`) sí que el té.
   */
  fun emitStale(
    missing: String,
    fromCache: Boolean,
  ) {
    observed.value =
      RemoteDevices(
        pro = pro,
        entries =
          entries.filterKeys { it != missing }.map { (id, entry) ->
            RemoteDeviceEntry(id, entry.sealedName, entry.addedAt, entry.lastSyncAt)
          },
        fromCache = fromCache,
      )
  }

  private fun publish() {
    observed.value = current()
  }

  /** El que hi ha al servidor ara mateix. */
  private fun current(): RemoteDevices =
    RemoteDevices(
      pro = pro,
      entries =
        entries.map { (id, entry) ->
          RemoteDeviceEntry(id, entry.sealedName, entry.addedAt, entry.lastSyncAt)
        },
    )

  /** Les regles: el mapa només pot créixer fins al límit del pla que queda després de l'escriptura. */
  private fun allowed(
    sizeBefore: Int,
    sizeAfter: Int,
    proAfter: Boolean,
  ): Boolean = sizeAfter <= sizeBefore || sizeAfter <= if (proAfter) PRO_DEVICES else FREE_DEVICES

  override suspend fun loadDevices(uid: String): RemoteResult<RemoteDevices> {
    failure?.let { return it }
    return RemoteResult.Ok(current())
  }

  override fun observeDevices(uid: String): Flow<RemoteDevices> = observed

  override suspend fun joinDevice(
    uid: String,
    device: RemoteDevice,
    removing: String?,
  ): JoinResult {
    failure?.let {
      return when (it) {
        RemoteResult.Offline -> JoinResult.Offline
        else -> JoinResult.Failed
      }
    }
    val after = LinkedHashMap(entries)
    if (removing != null && removing != device.id) after.remove(removing)
    val now = ++clock
    val known = after[device.id]
    if (known == null) {
      after[device.id] = Entry(device.sealedName, now, now)
    } else {
      known.lastSyncAt = now
    }
    if (!allowed(entries.size, after.size, device.pro)) return JoinResult.Full
    entries.clear()
    entries.putAll(after)
    pro = device.pro
    publish()
    return JoinResult.Joined
  }

  override suspend fun removeDevice(
    uid: String,
    id: String,
  ): RemoteResult<Unit> {
    failure?.let { return it }
    entries.remove(id)
    publish()
    return RemoteResult.Ok(Unit)
  }

  override suspend fun renameDevice(
    uid: String,
    id: String,
    sealedName: ByteArray,
  ): RemoteResult<Unit> {
    failure?.let { return it }
    entries[id]?.sealedName = sealedName
    publish()
    return RemoteResult.Ok(Unit)
  }

  override suspend fun touchDevice(
    uid: String,
    id: String,
  ): RemoteResult<Unit> {
    failure?.let { return it }
    entries[id]?.lastSyncAt = ++clock
    publish()
    return RemoteResult.Ok(Unit)
  }

  override suspend fun setPlan(
    uid: String,
    pro: Boolean,
  ): RemoteResult<Unit> {
    failure?.let { return it }
    this.pro = pro
    publish()
    return RemoteResult.Ok(Unit)
  }

  private companion object {
    const val FREE_DEVICES = 2
    const val PRO_DEVICES = 5
  }
}

class FakeDeviceStore(
  private val deviceId: String = "device-1",
  var registered: Boolean = false,
) : DeviceStore {
  override suspend fun deviceId(): String = deviceId

  override suspend fun isRegistered(): Boolean = registered

  override suspend fun setRegistered(value: Boolean) {
    registered = value
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

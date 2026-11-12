package com.alorma.contactnotes.data.sync.engine

import com.alorma.contactnotes.data.sync.ApplyResult
import com.alorma.contactnotes.data.sync.LocalState
import com.alorma.contactnotes.data.sync.LocalSyncSource
import com.alorma.contactnotes.data.sync.RemoteRecord
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SyncCursorStore
import com.alorma.contactnotes.data.sync.SyncOutcome
import com.alorma.contactnotes.data.sync.SyncRemote
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.record.ChannelRecord
import com.alorma.contactnotes.data.sync.record.EntryRecord
import com.alorma.contactnotes.data.sync.record.PersonRecord
import com.alorma.contactnotes.data.sync.record.RecordCodec
import com.alorma.contactnotes.data.sync.record.SyncRecord
import com.alorma.contactnotes.data.tracking.ErrorSites

/** Un registre que no s'ha pogut desxifrar o llegir. Sense missatge: no ha de portar mai cap dada. */
internal class UnreadableRecordException : Exception()

/**
 * La baixada, una execució per instància. Passa per pàgines de [pageSize] ordenades per `updatedAt` del servidor i
 * desa el cursor després de cada una, de manera que un procés mort continua per on era.
 *
 * Les referències desordenades (una entrada abans de la seva persona) esperen en memòria. El cursor no passa mai del
 * primer registre que espera: si el procés mor, la baixada següent el torna a demanar. En acabar una baixada completa,
 * el que encara espera és d'una persona esborrada i es descarta.
 *
 * Límit conegut: un esborrat de persona baixa en cascada a les seves entrades i tasques locals, i una d'aquestes
 * editada aquí després de l'esborrat (i encara sense pujar) es perd amb la persona.
 *
 * Els canals propis repetits (mateix grup i nom, `syncId` diferent) es fusionen en el de `syncId` més petit. Si el
 * guanyador és el local, l'àlies del remot viu en memòria durant la baixada i el remot deixa un tombstone. Si un
 * procés mor entre dues pàgines, les entrades que encara apuntin al remot esperaran i es descartaran; l'altre
 * dispositiu les torna a pujar amb el canal guanyador, perquè en adoptar-lo les ha segellat.
 */
internal class RecordPuller(
  private val local: LocalSyncSource,
  private val remote: SyncRemote,
  private val cursors: SyncCursorStore,
  private val clock: () -> Long,
  private val onError: (Throwable, String) -> Unit,
  private val pageSize: Int,
) {
  /** [record] és `null` si és un esborrat, i llavors [deletedAt] hi és. */
  private class Incoming(
    val meta: RemoteRecord,
    val record: SyncRecord?,
    val deletedAt: Long?,
  ) {
    /** Canals primer, després persones, després entrades i tasques, i els esborrats al final. */
    val order: Int =
      when (record) {
        is ChannelRecord -> 0
        is PersonRecord -> 1
        null -> 3
        else -> 2
      }
  }

  private val waiting = linkedMapOf<String, Incoming>()
  private val aliases = mutableMapOf<String, String>()
  private lateinit var ownDeviceId: String

  suspend fun pull(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit,
  ): SyncOutcome {
    ownDeviceId = cursors.deviceId()
    val startCursor = cursors.cursor()
    var since = maxOf(0L, startCursor - MARGIN_MILLIS)
    var last = startCursor
    var done = 0
    val seen = HashMap<String, Long>()
    while (true) {
      val page =
        when (val result = remote.pull(uid, since, pageSize)) {
          is RemoteResult.Ok -> result.value
          RemoteResult.Offline -> return SyncOutcome.Offline
          RemoteResult.Failed -> return SyncOutcome.Failed
        }
      // El marge i els empats de temps fan que un registre pugui tornar: el ja vist no es torna a aplicar
      val fresh = page.filter { seen.put(it.id, it.updatedAt) != it.updatedAt }
      fresh
        .mapNotNull { parse(it, cipher) }
        .sortedBy { it.order }
        .forEach { handle(it) }
      retryWaiting()
      done += fresh.size
      onProgress(done, null)
      val top = page.maxOfOrNull { it.updatedAt }
      if (top != null) last = maxOf(last, top)
      cursors.setCursor(safeCursor(last))
      if (top == null || page.size < pageSize) break
      // Un pas enrere perquè una pàgina no parteixi un grup d'escriptures amb la mateixa hora
      since = if (top - 1 > since) top - 1 else top
    }
    waiting.clear()
    cursors.setCursor(last)
    return SyncOutcome.Done
  }

  private fun safeCursor(last: Long): Long {
    val earliest = waiting.values.minOfOrNull { it.meta.updatedAt } ?: return last
    return minOf(last, earliest - 1)
  }

  private fun parse(
    meta: RemoteRecord,
    cipher: RecordCipher,
  ): Incoming? {
    val bytes = cipher.decrypt(meta.id, meta.sealed)
    val incoming =
      when {
        bytes == null -> null
        meta.deleted -> RecordCodec.decodeDeleted(bytes)?.let { Incoming(meta, null, it.deletedAt) }
        else -> RecordCodec.decode(bytes)?.let { Incoming(meta, it, null) }
      }
    if (incoming == null) onError(UnreadableRecordException(), ErrorSites.SYNC_RECORD)
    return incoming
  }

  private suspend fun retryWaiting() {
    waiting.values
      .toList()
      .sortedBy { it.order }
      .forEach { handle(it) }
  }

  private suspend fun handle(item: Incoming) {
    val id = item.meta.id
    val deletedAt = item.deletedAt
    if (deletedAt != null) {
      waiting.remove(id)
      val state = local.localState(id)
      if (state is LocalState.Live) {
        if (ConflictRules.remoteDeletionWins(state, deletedAt)) {
          local.applyRemoteDeletion(id, state.type)
        } else {
          reassertIfOverwritten(id, state, item.meta.by)
        }
      }
      return
    }
    val record = withAlias(item.record!!)
    val state = local.localState(id)
    if (!ConflictRules.remoteEditWins(state, record.modifiedAt, item.meta.by, ownDeviceId)) {
      waiting.remove(id)
      if (state is LocalState.Live) reassertIfOverwritten(id, state, item.meta.by)
      return
    }
    when (val result = local.applyRemote(id, record)) {
      ApplyResult.Applied -> waiting.remove(id)
      ApplyResult.MissingReference -> waiting[id] = item
      ApplyResult.Unreadable -> {
        waiting.remove(id)
        onError(UnreadableRecordException(), ErrorSites.SYNC_RECORD)
      }
      is ApplyResult.DuplicateChannel -> {
        waiting.remove(id)
        mergeChannel(id, record, result.localSyncId)
      }
    }
  }

  /**
   * El registre baixat ha perdut contra una fila local ja pujada: vol dir que una escriptura més antiga d'un altre
   * dispositiu ha caigut a sobre de la nostra al núvol. Sense tornar-la a pujar, cada dispositiu es quedaria amb la seva
   * i el núvol amb la que perd. Si la fila és pendent, ja pujarà; si el registre és nostre, no hi ha res a corregir.
   */
  private suspend fun reassertIfOverwritten(
    id: String,
    state: LocalState.Live,
    by: String,
  ) {
    if (ConflictRules.mustReassert(state, by, ownDeviceId)) local.markDirty(id, state.type)
  }

  private suspend fun mergeChannel(
    remoteSyncId: String,
    record: SyncRecord,
    localSyncId: String,
  ) {
    if (localSyncId < remoteSyncId) {
      aliases[remoteSyncId] = localSyncId
      // Perquè el canal remot es retiri també als altres dispositius
      local.addTombstone(remoteSyncId, clock())
    } else {
      local.adoptChannelSyncId(localSyncId, remoteSyncId, clock())
      local.applyRemote(remoteSyncId, record)
    }
  }

  private fun withAlias(record: SyncRecord): SyncRecord {
    if (record !is EntryRecord) return record
    val alias = record.customChannelId?.let { aliases[it] } ?: return record
    return record.copy(customChannelId = alias)
  }

  private companion object {
    /** Dues escriptures simultànies poden arribar desordenades: es demana un tros enrere del cursor. */
    const val MARGIN_MILLIS = 60_000L
  }
}

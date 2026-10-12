package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.shared.WearSnapshotCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Publica l'instantani al rellotge cada cop que canvien les dades (persones, entrades o Bloqueig de l'app) i quan el
 * rellotge es connecta. No registra mai contingut: ni al log ni en cap error.
 */
@OptIn(FlowPreview::class)
class WearSnapshotPublisher(
  private val repository: ConversesRepository,
  private val settings: SettingsRepository,
  private val link: WearLink,
  private val scope: CoroutineScope,
  private val clock: () -> Long = System::currentTimeMillis,
  private val debounceMillis: Long = DEBOUNCE_MILLIS,
) {
  fun start() {
    scope.launch {
      // Una primera passada en arrencar i una de nova cada cop que el rellotge es connecta: tornen a publicar encara
      // que no hi hagi canvis, perquè el rellotge que torna d'estar fora de línia quedi al dia.
      val connections =
        link.watchAvailable
          .distinctUntilChanged()
          .filter { it }
          .map { }
      merge(flowOf(Unit), connections).collectLatest { snapshots().collect(::publish) }
    }
  }

  private fun snapshots(): Flow<WearSnapshot> =
    combine(
      repository.observePeople(),
      repository.observeRecentEntriesForWear(),
      settings.settings.map { it.appLock }.distinctUntilChanged(),
    ) { people, entries, appLock ->
      WearSnapshotBuilder.build(people, entries, appLockActive = appLock, now = clock())
    }.debounce(debounceMillis)
      .distinctUntilChanged { old, new -> old.copy(generatedAt = 0) == new.copy(generatedAt = 0) }

  private suspend fun publish(snapshot: WearSnapshot) {
    try {
      link.publish(WearSnapshotCodec.encode(snapshot).toByteArray(Charsets.UTF_8))
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // El següent canvi o la propera connexió ho tornarà a provar; no es registra res perquè podria dur contingut
    }
  }

  private companion object {
    const val DEBOUNCE_MILLIS = 500L
  }
}

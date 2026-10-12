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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Publica l'instantani al rellotge cada cop que canvien les dades (persones, entrades o Bloqueig de l'app), però només
 * si hi ha un rellotge emparellat amb l'app. No registra mai contingut: ni al log ni en cap error.
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
      // Només es publica si hi ha un rellotge amb l'app. Cada cop que n'apareix un es torna a publicar encara que no hi
      // hagi canvis, i si desapareix s'esborra l'instantani de la Data Layer perquè no hi quedi res de les persones.
      link.watchPresent.distinctUntilChanged().collectLatest { present ->
        if (present) snapshots().collect(::publish) else clear()
      }
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

  private suspend fun clear() {
    try {
      link.clear()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // Si no hi ha Play Services no hi ha res a esborrar
    }
  }

  private companion object {
    const val DEBOUNCE_MILLIS = 500L
  }
}

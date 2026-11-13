package com.alorma.contactnotes.data.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Què fa la sincronització mentre l'app és visible. Només decideix quan: baixa en començar i quan algú altre escriu, i
 * puja [debounceMs] després de l'últim canvi local. Si falla, torna a provar amb l'espera creixent [backoffMs] (l'última
 * es repeteix). És una classe sense Android, amb el temps que dóna `delay`, perquè es pugui provar amb temps virtual.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SyncLoop(
  private val actions: SyncActions,
  private val debounceMs: Long = 2_000L,
  private val backoffMs: List<Long> = listOf(5_000L, 15_000L, 60_000L, 300_000L),
) {
  fun backoff(failures: Int): Long = backoffMs[failures.coerceIn(0, backoffMs.lastIndex)]

  /** Corre fins que el cancel·lin (l'app passa a segon pla o es tanca la sessió). */
  suspend fun run() =
    coroutineScope {
      launch { retrying { actions.pull() } }
      launch { listen() }
      launch { pushOnChanges() }
      launch { watchDevices() }
    }

  /** Si aquest dispositiu desapareix de `devices`, el repositori passa a `Removed` i el bucle es cancel·la. */
  private suspend fun watchDevices() {
    var failures = 0
    while (true) {
      try {
        actions.watchDevices()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // Es reprèn amb l'espera
      }
      delay(backoff(failures++))
    }
  }

  private suspend fun listen() {
    var failures = 0
    while (true) {
      try {
        actions.remoteChanges().collectLatest {
          failures = 0
          retrying { actions.pull() }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // El listener ha caigut: es reprèn amb l'espera, sense tombar el bucle
      }
      delay(backoff(failures++))
    }
  }

  private suspend fun pushOnChanges() {
    actions.pendingCount
      .debounce(debounceMs)
      .filter { it > 0 }
      .collectLatest { pushUntilClean() }
  }

  /** Una edició feta mentre es pujava no canvia el recompte: es mira si encara en queden. */
  private suspend fun pushUntilClean() {
    var failures = 0
    while (true) {
      if (actions.push() == SyncOutcome.Done) {
        failures = 0
        if (actions.pendingCount.first() == 0) return
        delay(debounceMs)
      } else {
        delay(backoff(failures++))
      }
    }
  }

  private suspend fun retrying(block: suspend () -> SyncOutcome) {
    var failures = 0
    while (block() != SyncOutcome.Done) delay(backoff(failures++))
  }
}

/**
 * Engega i atura el [SyncLoop] segons si l'app és visible i el dispositiu és [SyncState.Ready] (no [SyncState.Removed]); en passar a segon pla
 * amb canvis pendents demana el worker de fons. Només es crea amb [SyncFlag] encès. Sense sessió no passa mai res: ni
 * bucle ni WorkManager.
 */
class SyncScheduler(
  private val repository: SyncRepository,
  private val enqueueWorker: () -> Unit,
  private val scope: CoroutineScope,
) {
  /** Amb el cicle de vida del procés (`ProcessLifecycleOwner`). */
  fun start(lifecycle: Lifecycle) {
    val visible = MutableStateFlow(false)
    lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
          visible.value = true
        }

        override fun onStop(owner: LifecycleOwner) {
          visible.value = false
        }
      },
    )
    start(visible)
  }

  fun start(visible: Flow<Boolean>) {
    scope.launch {
      combine(visible, repository.state.map { it is SyncState.Ready }) { shown, ready -> shown && ready }
        .distinctUntilChanged()
        .collectLatest { active -> if (active) SyncLoop(repository).run() }
    }
    scope.launch {
      visible.distinctUntilChanged().collect { shown ->
        if (!shown && repository.state.value is SyncState.Ready && repository.pendingCount.first() > 0) enqueueWorker()
      }
    }
  }
}

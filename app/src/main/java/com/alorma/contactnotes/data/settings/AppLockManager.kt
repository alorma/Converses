package com.alorma.contactnotes.data.settings

import android.os.SystemClock
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Decideix quan cal mostrar el bloqueig de l'app: en obrir-la en fred i després de `relockAfterSeconds` en segon pla. */
class AppLockManager(
  settingsRepository: SettingsRepository,
  private val tracker: EventTracker,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
  private val now: () -> Long = SystemClock::elapsedRealtime,
) {
  private val locked = MutableStateFlow(true)
  private var backgroundedAt: Long? = null

  /** El bloqueig que ve ara és per haver tornat del segon pla, no per l'arrencada. */
  private var relocking = false

  @Volatile
  private var latest = AppSettings()

  val showLock: StateFlow<Boolean> =
    combine(settingsRepository.settings, locked) { settings, locked ->
      settings.appLock && locked
    }.stateIn(scope, SharingStarted.Eagerly, false)

  init {
    scope.launch { settingsRepository.settings.collect { latest = it } }
    // Només quan passa de no bloquejada a bloquejada; el valor inicial de `stateIn` és `false`
    showLock
      .filter { it }
      .onEach {
        tracker.trackEvent(Events.AppLocked(relock = relocking))
        relocking = false
      }.launchIn(scope)
  }

  fun onAppStopped() {
    backgroundedAt = now()
  }

  fun onAppStarted() {
    val at = backgroundedAt ?: return
    val elapsedSeconds = (now() - at) / 1000
    if (elapsedSeconds >= latest.relockAfterSeconds && !locked.value) {
      relocking = true
      locked.value = true
    }
  }

  fun unlock() {
    locked.value = false
    relocking = false
  }

  /** El mòbil no té bloqueig de pantalla: no es pot desbloquejar, i qui crida desactiva el bloqueig de l'app. */
  fun onDeviceUnprotected() {
    tracker.trackEvent(Events.AppLockDisabled())
    unlock()
  }
}

package com.alorma.contactnotes.data.settings

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Decideix quan cal mostrar el bloqueig de l'app: en obrir-la en fred i després de `relockAfterSeconds` en segon pla. */
class AppLockManager(settingsRepository: SettingsRepository) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val locked = MutableStateFlow(true)
  private var backgroundedAt: Long? = null

  @Volatile
  private var latest = AppSettings()

  val showLock: StateFlow<Boolean> = combine(settingsRepository.settings, locked) { settings, locked ->
    settings.appLock && locked
  }.stateIn(scope, SharingStarted.Eagerly, false)

  init {
    scope.launch { settingsRepository.settings.collect { latest = it } }
  }

  fun onAppStopped() {
    backgroundedAt = SystemClock.elapsedRealtime()
  }

  fun onAppStarted() {
    val at = backgroundedAt ?: return
    val elapsedSeconds = (SystemClock.elapsedRealtime() - at) / 1000
    if (elapsedSeconds >= latest.relockAfterSeconds) locked.value = true
  }

  fun unlock() {
    locked.value = false
  }
}

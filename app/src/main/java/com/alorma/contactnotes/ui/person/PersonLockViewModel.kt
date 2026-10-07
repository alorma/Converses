package com.alorma.contactnotes.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estat del bloqueig. No carrega res de la persona: el detall té el seu propi `ViewModel`. */
class PersonLockViewModel : ViewModel() {
  private val _state = MutableStateFlow<LockState>(LockState.Waiting)
  val state: StateFlow<LockState> = _state.asStateFlow()

  /** Si ja s'ha fet la comprovació inicial; evita reobrir el prompt en girar la pantalla o tornar de segon pla. */
  var started = false
    private set

  private var countdownJob: Job? = null

  /** Comprova si el dispositiu pot autenticar. Retorna `true` si s'ha d'obrir el prompt. */
  fun begin(availabilityCode: Int): Boolean {
    started = true
    countdownJob?.cancel()
    val next = lockStateForAvailability(availabilityCode)
    _state.value = next
    return next == LockState.Waiting
  }

  fun onError(code: Int) {
    countdownJob?.cancel()
    val next = lockStateForError(code)
    _state.value = next
    if (next is LockState.LockedOut) {
      countdownJob = viewModelScope.launch {
        countdown(next.seconds).collect { _state.value = LockState.LockedOut(it) }
      }
    }
  }
}

package com.alorma.contactnotes.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Compta les pantalles que demanen `FLAG_SECURE`; l'activitat n'aplica el flag. */
class SecureWindow {
  private val requests = MutableStateFlow(0)

  val isRequested: Flow<Boolean> = requests.map { it > 0 }.distinctUntilChanged()

  fun acquire() = requests.update { it + 1 }

  fun release() = requests.update { (it - 1).coerceAtLeast(0) }
}

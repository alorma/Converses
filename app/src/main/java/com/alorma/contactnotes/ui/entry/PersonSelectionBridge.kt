package com.alorma.contactnotes.ui.entry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Porta la persona creada des de l'editor fins a la nota ràpida que l'ha demanada. */
class PersonSelectionBridge {
  private val _picked = MutableStateFlow<Long?>(null)
  val picked: StateFlow<Long?> = _picked

  fun post(personId: Long) {
    _picked.value = personId
  }

  fun clear() {
    _picked.value = null
  }
}

package com.alorma.contactnotes.ui.entry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persona triada a l'editor; `authenticated` vol dir que l'usuari ja s'ha autenticat en aquesta mateixa sessió. */
data class PersonPick(
  val personId: Long,
  val authenticated: Boolean,
)

/** Porta la persona creada des de l'editor fins a la nota ràpida que l'ha demanada. */
class PersonSelectionBridge {
  private val _picked = MutableStateFlow<PersonPick?>(null)
  val picked: StateFlow<PersonPick?> = _picked

  fun post(
    personId: Long,
    authenticated: Boolean = false,
  ) {
    _picked.value = PersonPick(personId, authenticated)
  }

  fun clear() {
    _picked.value = null
  }
}

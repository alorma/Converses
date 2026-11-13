package com.alorma.contactnotes.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.ui.tracking.Actions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * El que la sincronització porta a la pantalla sense que ningú ho demani, a nivell d'activitat: obrir les places
 * plenes (13l) i comptar-ho. Només es crea amb SyncFlag encès.
 *
 * `sync_removed_seen` es compta una sola vegada per arribada a [SyncState.Removed]: un senyal es reinicia quan l'estat
 * deixa de ser «tret» (tornar a entrar, tancar la sessió). Viu a aquest ViewModel, que sobreviu a girar la pantalla,
 * i no es guarda enlloc: un procés nou amb el dispositiu ja tret el torna a comptar un cop.
 */
class SyncPromptsViewModel(
  private val repository: SyncRepository,
  private val tracker: EventTracker,
) : ViewModel() {
  /** Hi ha una pregunta de places: la ruta de 13l ha d'estar oberta. */
  val slotsOpen: StateFlow<Boolean> =
    repository.slotsPrompt
      .map { it != null }
      .stateIn(viewModelScope, SharingStarted.Eagerly, repository.slotsPrompt.value != null)

  private var removedCounted = false

  init {
    viewModelScope.launch {
      var wasOpen = false
      repository.slotsPrompt.collect { prompt ->
        val open = prompt != null
        if (open && !wasOpen) tracker.trackAction(Actions.SyncSlotsShown())
        wasOpen = open
      }
    }
    viewModelScope.launch {
      repository.state.collect { state ->
        if (state is SyncState.Removed) {
          if (!removedCounted) tracker.trackAction(Actions.SyncRemovedSeen())
          removedCounted = true
        } else {
          removedCounted = false
        }
      }
    }
  }
}

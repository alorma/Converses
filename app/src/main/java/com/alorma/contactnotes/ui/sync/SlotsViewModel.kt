package com.alorma.contactnotes.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.DeviceActionOutcome
import com.alorma.contactnotes.data.sync.SlotsPrompt
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.ui.tracking.Actions
import com.alorma.contactnotes.ui.tracking.DeviceRemovedFrom
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Places plenes (13l): triar quin dispositiu es treu, o deixar-ho. */
class SlotsViewModel(
  private val repository: SyncRepository,
  private val tracker: EventTracker,
) : ViewModel() {
  val prompt: StateFlow<SlotsPrompt?> =
    repository.slotsPrompt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.slotsPrompt.value)

  private val mutableWorking = MutableStateFlow(false)

  /** S'està treient el dispositiu triat: els botons no es poden tornar a prémer. */
  val working: StateFlow<Boolean> = mutableWorking.asStateFlow()

  private val messageChannel = Channel<SyncMessage>(Channel.BUFFERED)
  val messages: Flow<SyncMessage> = messageChannel.receiveAsFlow()

  /** «Treu-lo i continua». En acabar bé el repositori buida la pregunta i la ruta es tanca sola. */
  fun onRemoveAndContinue(id: String) {
    if (mutableWorking.value) return
    mutableWorking.value = true
    viewModelScope.launch {
      try {
        when (repository.chooseDeviceToRemove(id)) {
          DeviceActionOutcome.Done -> tracker.trackAction(Actions.SyncDeviceRemoved(DeviceRemovedFrom.SLOTS))
          DeviceActionOutcome.Offline -> messageChannel.send(SyncMessage.Offline)
          DeviceActionOutcome.Failed -> messageChannel.send(SyncMessage.Failed)
        }
      } finally {
        mutableWorking.value = false
      }
    }
  }

  /** «Ara no» (o enrere): tanca la sessió sense deixar res al núvol. */
  fun onNotNow() {
    viewModelScope.launch { repository.cancelSlots() }
  }
}

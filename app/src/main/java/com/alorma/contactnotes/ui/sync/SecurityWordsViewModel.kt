package com.alorma.contactnotes.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.ActivationOutcome
import com.alorma.contactnotes.data.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Què mostra la pantalla de les paraules. [words] és buit només el moment abans de tancar-se sola. */
data class SecurityWordsState(
  val words: List<String> = emptyList(),
  /** «Les he guardat en un lloc segur»; només es fa servir a l'activació. */
  val saved: Boolean = false,
  /** Pujant la clau: el botó queda desactivat i no es pot tornar a prémer. */
  val working: Boolean = false,
)

/** El que la pantalla ha de fer una sola vegada. */
enum class SecurityWordsEvent { Close, Offline, Failed }

/**
 * Les paraules de seguretat. Les llegeix una sola vegada en crear-se: si el repositori no les dona (el procés s'ha
 * restaurat sense haver passat per l'empremta), la pantalla es tanca sola. [activation] és el primer dispositiu, amb
 * la clau encara sense pujar.
 */
class SecurityWordsViewModel(
  private val activation: Boolean,
  private val repository: SyncRepository,
  /** Sobreviu al ViewModel: tancar la pantalla amb el gest o fora del diàleg ha de poder tancar la sessió igualment. */
  private val cleanupScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ViewModel() {
  private val mutableState = MutableStateFlow(SecurityWordsState(words = repository.securityWords().orEmpty()))
  val state: StateFlow<SecurityWordsState> = mutableState.asStateFlow()

  private val eventChannel = Channel<SecurityWordsEvent>(Channel.BUFFERED)
  val events: Flow<SecurityWordsEvent> = eventChannel.receiveAsFlow()

  init {
    if (mutableState.value.words.isEmpty()) {
      close()
    } else if (!activation) {
      viewModelScope.launch { repository.wordsReviewed() }
    }
  }

  fun onSavedChange(saved: Boolean) = mutableState.update { it.copy(saved = saved) }

  fun onContinue() {
    val current = mutableState.value
    if (!activation || !current.saved || current.working) return
    mutableState.update { it.copy(working = true) }
    viewModelScope.launch {
      when (repository.confirmActivation()) {
        // Amb AlreadyActive l'estat ja és «falta la clau»: la pantalla de sota ho resol
        ActivationOutcome.Done, ActivationOutcome.AlreadyActive -> eventChannel.send(SecurityWordsEvent.Close)
        ActivationOutcome.Offline -> failed(SecurityWordsEvent.Offline)
        ActivationOutcome.Failed -> failed(SecurityWordsEvent.Failed)
      }
    }
  }

  private suspend fun failed(event: SecurityWordsEvent) {
    mutableState.update { it.copy(working = false) }
    eventChannel.send(event)
  }

  /**
   * Tancar abans de continuar (a l'activació) tanca la sessió, i només després es tanca la pantalla perquè la de sota
   * no vegi un moment «Activating» i torni a demanar l'empremta.
   */
  fun close() {
    viewModelScope.launch {
      if (activation) repository.cancelActivation()
      eventChannel.send(SecurityWordsEvent.Close)
    }
  }

  override fun onCleared() {
    // Les paraules tornen a necessitar l'empremta
    repository.lockWords()
    if (activation) cleanupScope.launch { repository.cancelActivation() }
  }
}

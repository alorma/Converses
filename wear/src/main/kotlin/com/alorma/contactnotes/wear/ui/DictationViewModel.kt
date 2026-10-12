package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.wear.data.SpeechError
import com.alorma.contactnotes.wear.data.SpeechEvent
import com.alorma.contactnotes.wear.data.SpeechSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DictationUiState {
  /** Escoltant: [text] és el parcial en directe i [level] l'amplitud del micròfon (0..1). */
  data class Listening(
    val text: String = "",
    val level: Float = 0f,
  ) : DictationUiState

  /** Dictat acabat: [text] és la transcripció definitiva (la fase 7 la passa a «Qui»). */
  data class Done(
    val text: String,
  ) : DictationUiState

  data class Failed(
    val reason: SpeechError,
  ) : DictationUiState
}

/**
 * Un dictat. El micròfon només s'obre amb [start], que la pantalla crida un cop té el permís, i es tanca quan el
 * ViewModel mor. Cap text surt d'aquí: viu en memòria fins que la fase 7 el desa.
 */
class DictationViewModel(
  private val speech: SpeechSource,
  private val languageTag: String,
) : ViewModel() {
  private val _state = MutableStateFlow<DictationUiState>(DictationUiState.Listening())
  val state: StateFlow<DictationUiState> = _state

  private var job: Job? = null

  fun start() {
    if (job?.isActive == true) return
    _state.value = DictationUiState.Listening()
    job =
      viewModelScope.launch {
        speech.listen(languageTag).collect { event ->
          _state.update { current -> current.after(event) }
        }
      }
  }

  /** «Acaba»: tanca l'escolta i espera el text definitiu; sense reconeixedor actiu, tanca amb el parcial. */
  fun finish() {
    val current = _state.value as? DictationUiState.Listening ?: return
    if (job?.isActive == true) {
      speech.finish()
    } else {
      _state.value =
        if (current.text.isBlank()) {
          DictationUiState.Failed(
            SpeechError.NoSpeech,
          )
        } else {
          DictationUiState.Done(current.text)
        }
    }
  }

  fun retry() {
    job?.cancel()
    job = null
    start()
  }

  private fun DictationUiState.after(event: SpeechEvent): DictationUiState =
    when (event) {
      is SpeechEvent.Partial -> (this as? DictationUiState.Listening)?.copy(text = event.text) ?: this
      is SpeechEvent.Level -> (this as? DictationUiState.Listening)?.copy(level = event.value) ?: this
      is SpeechEvent.Final ->
        if (event.text.isBlank()) DictationUiState.Failed(SpeechError.NoSpeech) else DictationUiState.Done(event.text)
      is SpeechEvent.Error -> DictationUiState.Failed(event.reason)
    }

  override fun onCleared() {
    job?.cancel()
  }
}

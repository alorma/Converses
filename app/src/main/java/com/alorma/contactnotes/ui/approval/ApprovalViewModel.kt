package com.alorma.contactnotes.ui.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.wear.WearApprovalCoordinator
import com.alorma.contactnotes.shared.WEAR_APPROVAL_TTL_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Estat de l'aprovació. No porta mai res de la persona: ni el nom ni cap contingut. */
sealed interface ApprovalUiState {
  /** El `BiometricPrompt` és obert (o s'està obrint): no es mostra cap pantalla pròpia. */
  data object Prompting : ApprovalUiState

  /** L'usuari ha cancel·lat el prompt: ara sí que es veu la pantalla amb «Confirma» (reobre el prompt) i «Denega». */
  data object Confirm : ApprovalUiState

  /** La petició ja no es pot completar (caducada, cancel·lada al rellotge o desconeguda): es tanca amb un missatge. */
  data object Failed : ApprovalUiState

  /** Aprovada o denegada: la pantalla es tanca sense missatge (després d'aprovar no cal cap pantalla de «Fet»). */
  data object Closed : ApprovalUiState
}

class ApprovalViewModel(
  private val requestId: String,
  private val coordinator: WearApprovalCoordinator,
) : ViewModel() {
  // Una petició caducada, cancel·lada o repetida (p. ex. en obrir una notificació antiga) no té res a aprovar
  private val _state =
    MutableStateFlow<ApprovalUiState>(
      if (coordinator.isPending(requestId)) ApprovalUiState.Prompting else ApprovalUiState.Failed,
    )
  val state: StateFlow<ApprovalUiState> = _state.asStateFlow()

  private var promptRequested = _state.value == ApprovalUiState.Prompting

  init {
    if (_state.value == ApprovalUiState.Prompting) {
      // Si el rellotge cancel·la, o la petició caduca, amb la pantalla (o el prompt) oberts, es tanca sola
      viewModelScope.launch {
        coordinator.cancelledByWatch.first { it == requestId }
        closeIfOpen()
      }
      viewModelScope.launch {
        delay(WEAR_APPROVAL_TTL_MS + EXPIRY_MARGIN_MS)
        if (!coordinator.isPending(requestId)) closeIfOpen()
      }
    }
  }

  private fun closeIfOpen() {
    if (_state.value == ApprovalUiState.Prompting || _state.value == ApprovalUiState.Confirm) {
      _state.value = ApprovalUiState.Closed
    }
  }

  /** L'activitat el crida en veure [ApprovalUiState.Prompting]: només un cop per intent (girar la pantalla no reobre res). */
  fun takePromptRequest(): Boolean = promptRequested.also { promptRequested = false }

  /** Persona i rellotge per al text del prompt; `null` si la petició ja no és viva. */
  suspend fun details() = coordinator.details(requestId)

  /** La biometria ha anat bé. */
  fun onAuthenticated() {
    if (_state.value == ApprovalUiState.Closed || _state.value == ApprovalUiState.Failed) return
    viewModelScope.launch {
      _state.value = if (coordinator.approve(requestId)) ApprovalUiState.Closed else ApprovalUiState.Failed
    }
  }

  /** L'usuari ha cancel·lat el prompt (o ha fallat): se li ofereix confirmar de nou o denegar, no es denega sense més. */
  fun onPromptCancelled() {
    if (_state.value == ApprovalUiState.Prompting) _state.value = ApprovalUiState.Confirm
  }

  /** «Confirma» a la pantalla: es reobre el prompt. */
  fun onRetry() {
    if (_state.value != ApprovalUiState.Confirm) return
    promptRequested = true
    _state.value = ApprovalUiState.Prompting
  }

  /** «Denega»: el rellotge rep «Denegat». També quan el dispositiu no té biometria ni bloqueig de pantalla. */
  fun onDeny() {
    if (_state.value == ApprovalUiState.Closed || _state.value == ApprovalUiState.Failed) return
    viewModelScope.launch {
      coordinator.deny(requestId)
      _state.value = ApprovalUiState.Closed
    }
  }
}

// Una mica més que la vigència: el mòbil valida el temps i la petició ja no hi és
private const val EXPIRY_MARGIN_MS = 1_000L

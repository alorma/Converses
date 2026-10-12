package com.alorma.contactnotes.ui.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.wear.WearApprovalCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estat de la pantalla d'aprovació. No porta mai res de la persona: ni el nom ni cap contingut. */
sealed interface ApprovalUiState {
  /** Espera que l'usuari toqui «Confirma» (que obre el `BiometricPrompt`) o «Denega». */
  data object Confirm : ApprovalUiState

  /** Aprovada: el rellotge ja té les entrades. */
  data object Done : ApprovalUiState

  /** La petició ja no es pot completar (caducada, cancel·lada al rellotge o desconeguda): es tanca amb un missatge. */
  data object Failed : ApprovalUiState

  /** Denegada o descartada: la pantalla es tanca sense missatge. */
  data object Closed : ApprovalUiState
}

class ApprovalViewModel(
  private val requestId: String,
  private val coordinator: WearApprovalCoordinator,
) : ViewModel() {
  private val _state =
    MutableStateFlow<ApprovalUiState>(
      // Una petició caducada, cancel·lada o repetida (p. ex. en obrir una notificació antiga) no té res a aprovar
      if (coordinator.isPending(requestId)) ApprovalUiState.Confirm else ApprovalUiState.Failed,
    )
  val state: StateFlow<ApprovalUiState> = _state.asStateFlow()

  /** La biometria ha anat bé. */
  fun onAuthenticated() {
    if (_state.value != ApprovalUiState.Confirm) return
    viewModelScope.launch {
      _state.value = if (coordinator.approve(requestId)) ApprovalUiState.Done else ApprovalUiState.Failed
    }
  }

  /** «Denega», cancel·lar el prompt o fallar la biometria: el rellotge rep «Denegat». */
  fun onDeny() {
    if (_state.value != ApprovalUiState.Confirm) return
    viewModelScope.launch {
      coordinator.deny(requestId)
      _state.value = ApprovalUiState.Closed
    }
  }
}

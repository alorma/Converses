package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import com.alorma.contactnotes.wear.data.PhoneInstall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CompanionUiState {
  /** Encara no se sap si l'app és al mòbil: no es mostra res. */
  data object Loading : CompanionUiState

  /** L'app és al mòbil, o no hi ha mòbil emparellat: l'app funciona com sempre. */
  data object Ready : CompanionUiState

  /** Pantalla 17: el mòbil emparellat no té l'app. */
  data class Missing(
    val node: CompanionNode,
  ) : CompanionUiState

  /** Pantalla 18: s'ha obert Google Play al mòbil. */
  data object Opened : CompanionUiState
}

class CompanionViewModel(
  private val install: PhoneInstall,
) : ViewModel() {
  private val opened = MutableStateFlow(false)

  val state: StateFlow<CompanionUiState> =
    combine(install.state, opened) { companion, wasOpened ->
      when (companion) {
        CompanionState.Present, CompanionState.NoDevice -> CompanionUiState.Ready
        is CompanionState.Missing ->
          if (wasOpened) CompanionUiState.Opened else CompanionUiState.Missing(companion.node)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), CompanionUiState.Loading)

  /** «Obre al mòbil». Si el sistema no ho pot fer, es continua a la pantalla 17. */
  fun openOnPhone(node: CompanionNode) {
    viewModelScope.launch {
      if (install.openPlayOnPhone(node)) opened.value = true
    }
  }

  private companion object {
    const val StopTimeoutMillis = 5_000L
  }
}

package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import com.alorma.contactnotes.wear.data.PhoneInstall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CompanionUiState {
  /** Encara no se sap si l'app és al mòbil: no es mostra res. */
  data object Loading : CompanionUiState

  /** L'app és al mòbil, o no hi ha mòbil però hi ha dades guardades: l'app funciona com sempre. */
  data object Ready : CompanionUiState

  /** Sense cap mòbil emparellat i sense dades sincronitzades: no hi ha res a mostrar, només cal dir què fa falta. */
  data object NoPhone : CompanionUiState

  /** Pantalla 17: el mòbil emparellat no té l'app. */
  data class Missing(
    val node: CompanionNode,
  ) : CompanionUiState

  /** Pantalla 18: s'ha obert Google Play al mòbil. */
  data object Opened : CompanionUiState
}

class CompanionViewModel(
  private val install: PhoneInstall,
  hasData: Flow<Boolean>,
) : ViewModel() {
  private val opened = MutableStateFlow(false)

  val state: StateFlow<CompanionUiState> =
    combine(install.state, opened, hasData) { companion, wasOpened, synced ->
      when (companion) {
        CompanionState.Present -> CompanionUiState.Ready
        // Amb dades guardades l'app funciona com sempre; sense dades ni mòbil, no hi ha res a fer sense ell
        CompanionState.NoDevice -> if (synced) CompanionUiState.Ready else CompanionUiState.NoPhone
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

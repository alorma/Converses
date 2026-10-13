package com.alorma.contactnotes.ui.watchinstall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.data.wear.WatchInstall
import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface WatchInstallUiState {
  data object Hidden : WatchInstallUiState

  /** Pantalla 19: hi ha un rellotge Wear OS connectat sense l'app. [watchName] és `null` si el sistema no el coneix. */
  data class Propose(
    val watchName: String?,
  ) : WatchInstallUiState

  /** Pantalla 20: s'ha obert Google Play al rellotge. */
  data class Finish(
    val watchName: String?,
  ) : WatchInstallUiState
}

/** Proposa instal·lar l'app al rellotge emparellat que no la té. Només mira l'estat en arrencar. */
class WatchInstallViewModel(
  private val install: WatchInstall,
  private val settings: SettingsRepository,
) : ViewModel() {
  private val _state = MutableStateFlow<WatchInstallUiState>(WatchInstallUiState.Hidden)
  val state: StateFlow<WatchInstallUiState> = _state.asStateFlow()

  private var node: CompanionNode? = null

  init {
    viewModelScope.launch {
      if (settings.settings.first().watchInstallDismissed) return@launch
      val companion = install.state()
      if (companion is CompanionState.Missing) {
        node = companion.node
        _state.value = WatchInstallUiState.Propose(companion.node.readableName())
      }
    }
  }

  /** «Instal·la al rellotge». Si el sistema no pot obrir Play, es continua a la pantalla 19. */
  fun onInstall() {
    val target = node ?: return
    viewModelScope.launch {
      if (install.openPlayOnWatch(target)) _state.value = WatchInstallUiState.Finish(target.readableName())
    }
  }

  /** «Ara no»: es torna a proposar a la propera arrencada. */
  fun onNotNow() {
    _state.value = WatchInstallUiState.Hidden
  }

  /** «No m'ho tornis a demanar»: preferència local, mai a Room. */
  fun onNeverAgain() {
    _state.value = WatchInstallUiState.Hidden
    viewModelScope.launch { settings.setWatchInstallDismissed(true) }
  }

  fun onDone() {
    _state.value = WatchInstallUiState.Hidden
  }
}

private fun CompanionNode.readableName(): String? = displayName?.takeIf { it.isNotBlank() }

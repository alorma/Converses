package com.alorma.contactnotes.ui.sync

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.SignInOutcome
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.ui.tracking.Actions
import com.alorma.contactnotes.ui.tracking.SyncSignInResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** El que la pantalla ha de dir una sola vegada, amb un snackbar. */
enum class SyncMessage { SignInOffline, SignInFailed }

/** La pantalla «Sincronització»: l'estat del compte i les accions que no són de cap subpantalla. */
class SyncViewModel(
  private val repository: SyncRepository,
  private val tracker: EventTracker,
  /** Sobreviu al ViewModel: la cancel·lació de [onCleared] ha d'acabar encara que ja no hi sigui. */
  private val cleanupScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ViewModel() {
  val state: StateFlow<SyncState> get() = repository.state

  /** `null` sense sessió o mentre es carrega. */
  val wordsReviewedAt: StateFlow<Long?> =
    repository.wordsReviewedAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  private val messageChannel = Channel<SyncMessage>(Channel.BUFFERED)
  val messages: Flow<SyncMessage> = messageChannel.receiveAsFlow()

  /** Si l'empremta de l'activació ja s'ha demanat; cada inici de sessió en torna a demanar una. */
  private var activationPrompted = false

  fun onSignIn(activity: Activity) {
    activationPrompted = false
    tracker.trackAction(Actions.SyncSignInStarted())
    viewModelScope.launch {
      val outcome = repository.signIn(activity)
      tracker.trackAction(Actions.SyncSignInFinished(outcome.tracked()))
      when (outcome) {
        SignInOutcome.Done, SignInOutcome.Cancelled -> Unit
        SignInOutcome.Offline -> messageChannel.send(SyncMessage.SignInOffline)
        SignInOutcome.Failed -> messageChannel.send(SyncMessage.SignInFailed)
      }
    }
  }

  private fun SignInOutcome.tracked(): SyncSignInResult =
    when (this) {
      SignInOutcome.Done ->
        if (repository.state.value is SyncState.NeedsKey) SyncSignInResult.NEEDS_KEY else SyncSignInResult.ACTIVATING
      SignInOutcome.Cancelled -> SyncSignInResult.CANCELLED
      SignInOutcome.Offline -> SyncSignInResult.OFFLINE
      SignInOutcome.Failed -> SyncSignInResult.FAILED
    }

  /**
   * `true` només la primera vegada de cada activació: viu al ViewModel perquè girar la pantalla no torni a obrir el
   * diàleg del sistema, però un segon inici de sessió des de la mateixa pantalla sí que l'obri.
   */
  fun takeActivationPrompt(): Boolean {
    if (activationPrompted) return false
    activationPrompted = true
    return true
  }

  /** L'empremta ha anat bé: la pantalla de les paraules ja les pot demanar. */
  fun onWordsUnlocked() = repository.unlockWords()

  /** A l'activació, cancel·lar l'empremta o tancar abans de continuar tanca la sessió. */
  fun onCancelActivation() {
    viewModelScope.launch {
      if (repository.cancelActivation()) tracker.trackAction(Actions.SyncActivationCancelled())
    }
  }

  /** «Tanca la sessió» amb la sessió activa, o «Canvia» el compte quan falten les paraules. */
  fun onSignOut() {
    val needsKey = repository.state.value is SyncState.NeedsKey
    viewModelScope.launch {
      repository.signOut()
      tracker.trackAction(Actions.SyncSignedOut(needsKey))
    }
  }

  /**
   * Sortir de «Sincronització» (enrere) amb una activació a mitges la cancel·la, com tancar les paraules: si no, la
   * sessió es quedaria oberta sense clau. Mentre les paraules són obertes, aquesta pantalla és a sota i no s'esborra.
   */
  override fun onCleared() {
    cleanupScope.launch {
      if (repository.cancelActivation()) tracker.trackAction(Actions.SyncActivationCancelled())
    }
  }
}

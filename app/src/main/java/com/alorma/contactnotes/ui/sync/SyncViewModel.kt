package com.alorma.contactnotes.ui.sync

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.SignInOutcome
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncState
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
) : ViewModel() {
  val state: StateFlow<SyncState> get() = repository.state

  /** `null` sense sessió o mentre es carrega. */
  val wordsReviewedAt: StateFlow<Long?> =
    repository.wordsReviewedAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  private val messageChannel = Channel<SyncMessage>(Channel.BUFFERED)
  val messages: Flow<SyncMessage> = messageChannel.receiveAsFlow()

  fun onSignIn(activity: Activity) {
    viewModelScope.launch {
      when (repository.signIn(activity)) {
        SignInOutcome.Done, SignInOutcome.Cancelled -> Unit
        SignInOutcome.Offline -> messageChannel.send(SyncMessage.SignInOffline)
        SignInOutcome.Failed -> messageChannel.send(SyncMessage.SignInFailed)
      }
    }
  }

  /** L'empremta ha anat bé: la pantalla de les paraules ja les pot demanar. */
  fun onWordsUnlocked() = repository.unlockWords()

  /** A l'activació, cancel·lar l'empremta o tancar abans de continuar tanca la sessió. */
  fun onCancelActivation() {
    viewModelScope.launch { repository.cancelActivation() }
  }

  fun onSignOut() {
    viewModelScope.launch { repository.signOut() }
  }
}

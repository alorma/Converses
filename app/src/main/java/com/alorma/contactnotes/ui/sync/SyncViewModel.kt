package com.alorma.contactnotes.ui.sync

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.DeviceActionOutcome
import com.alorma.contactnotes.data.sync.DeviceInfo
import com.alorma.contactnotes.data.sync.InitialSync
import com.alorma.contactnotes.data.sync.RejoinOutcome
import com.alorma.contactnotes.data.sync.SignInOutcome
import com.alorma.contactnotes.data.sync.SyncOutcome
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.sync.SyncStatus
import com.alorma.contactnotes.data.tracking.Action
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.ui.tracking.Actions
import com.alorma.contactnotes.ui.tracking.DeviceRemovedFrom
import com.alorma.contactnotes.ui.tracking.SyncSignInResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** El que la pantalla ha de dir una sola vegada, amb un snackbar. */
enum class SyncMessage { SignInOffline, SignInFailed, Offline, Failed }

/** Llargada màxima d'un nom de dispositiu. */
const val MAX_DEVICE_NAME = 40

/** Les places d'un compte sense Pro (la fila de Pro només surt amb aquest límit). */
const val FREE_DEVICE_SLOTS = 2

/** La pantalla «Sincronització»: l'estat del compte i les accions que no són de cap subpantalla. */
class SyncViewModel(
  private val repository: SyncRepository,
  private val tracker: EventTracker,
  /** Sobreviu al ViewModel: la cancel·lació de [onCleared] ha d'acabar encara que ja no hi sigui. */
  private val cleanupScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ViewModel() {
  val state: StateFlow<SyncState> get() = repository.state

  /** Com van les dades (13o): al dia, sincronitzant, sense connexió o error. */
  val status: StateFlow<SyncStatus> get() = repository.status

  /** La pujada o baixada de després d'activar (13g, 13h, 13m, 13m2); `null` quan no n'hi ha cap a la vista. */
  val initialSync: StateFlow<InitialSyncUi?> =
    repository.initialSync
      .map { it?.toUi() }
      .stateIn(viewModelScope, SharingStarted.Eagerly, repository.initialSync.value?.toUi())

  private fun InitialSync.toUi(): InitialSyncUi =
    when (this) {
      is InitialSync.Uploading -> InitialSyncUi.Uploading(progress)
      is InitialSync.Downloading -> InitialSyncUi.Downloading(progress)
      is InitialSync.MergePrompt -> InitialSyncUi.MergePrompt(entryCount)
      InitialSync.Done -> InitialSyncUi.Done(repository.deviceName().orEmpty())
      InitialSync.Offline -> InitialSyncUi.Offline
      InitialSync.Failed -> InitialSyncUi.Failed
    }

  init {
    // L'analítica de la pujada i la baixada inicials surt dels canvis d'estat. Només compta mentre aquesta pantalla
    // existeix: amb «Amaga» i la pantalla tancada, el resultat ja no es veu ni es compta.
    viewModelScope.launch {
      var previous: InitialSync? = repository.initialSync.value
      repository.initialSync.collect { current ->
        trackInitialTransition(previous, current)
        previous = current
      }
    }
  }

  private fun trackInitialTransition(
    previous: InitialSync?,
    current: InitialSync?,
  ) = initialSyncActions(previous, current).forEach(tracker::trackAction)

  /** «Amaga» o «Fet»: la feina, si encara n'hi ha, continua sola. */
  fun onInitialSyncDismissed() = repository.dismissInitialSync()

  fun onMergeContinue() = repository.continueMerge()

  /** Els dispositius del compte (13o), amb aquest primer. Buit sense sessió o mentre es carrega. */
  val devices: StateFlow<List<DeviceInfo>> =
    repository.devices
      .map { list -> list.sortedByDescending { it.isThis } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Les places del compte (2, o 5 amb Pro). */
  val deviceLimit: StateFlow<Int> =
    repository.deviceLimit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FREE_DEVICE_SLOTS)

  /** «Canvia el nom». El nom no es compta mai. */
  fun onRename(
    id: String,
    name: String,
  ) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      when (repository.renameDevice(id, trimmed.take(MAX_DEVICE_NAME))) {
        DeviceActionOutcome.Done -> tracker.trackAction(Actions.SyncDeviceRenamed())
        DeviceActionOutcome.Offline -> messageChannel.send(SyncMessage.Offline)
        DeviceActionOutcome.Failed -> messageChannel.send(SyncMessage.Failed)
      }
    }
  }

  /** «Treu» de la llista, ja confirmat. */
  fun onRemove(id: String) {
    viewModelScope.launch {
      when (repository.removeDevice(id)) {
        DeviceActionOutcome.Done -> tracker.trackAction(Actions.SyncDeviceRemoved(DeviceRemovedFrom.LIST))
        DeviceActionOutcome.Offline -> messageChannel.send(SyncMessage.Offline)
        DeviceActionOutcome.Failed -> messageChannel.send(SyncMessage.Failed)
      }
    }
  }

  /** «Torna a entrar». Amb les places plenes (`Full`) la pantalla de 13l s'obre sola. */
  fun onRejoin() {
    viewModelScope.launch {
      val outcome = repository.rejoin()
      tracker.trackAction(Actions.SyncRejoined(outcome))
      when (outcome) {
        RejoinOutcome.Done, RejoinOutcome.Full -> Unit
        RejoinOutcome.Offline -> messageChannel.send(SyncMessage.Offline)
        RejoinOutcome.Failed -> messageChannel.send(SyncMessage.Failed)
      }
    }
  }

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

/**
 * Què es compta quan la pujada o baixada inicial passa de [previous] a [current]: el diàleg de fusió en aparèixer, i
 * el resultat de cada fase en acabar. A la fusió, la pujada acaba bé quan comença la baixada.
 */
internal fun initialSyncActions(
  previous: InitialSync?,
  current: InitialSync?,
): List<Action> {
  val actions = mutableListOf<Action>()
  if (current is InitialSync.MergePrompt && previous !is InitialSync.MergePrompt) actions += Actions.SyncMergeShown()
  val outcome =
    when (current) {
      InitialSync.Done -> SyncOutcome.Done
      InitialSync.Offline -> SyncOutcome.Offline
      InitialSync.Failed -> SyncOutcome.Failed
      is InitialSync.Downloading -> SyncOutcome.Done.takeIf { previous is InitialSync.Uploading }
      else -> null
    } ?: return actions
  when (previous) {
    is InitialSync.Uploading -> actions += Actions.SyncUploadFinished(outcome)
    is InitialSync.Downloading -> actions += Actions.SyncDownloadFinished(outcome)
    else -> Unit
  }
  return actions
}

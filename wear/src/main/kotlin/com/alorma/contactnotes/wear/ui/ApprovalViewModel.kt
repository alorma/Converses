package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.shared.WEAR_APPROVAL_TTL_MS
import com.alorma.contactnotes.shared.WearApprovalDecision
import com.alorma.contactnotes.shared.WearApprovalResult
import com.alorma.contactnotes.wear.data.ApprovalAckResult
import com.alorma.contactnotes.wear.data.ApprovalClient
import com.alorma.contactnotes.wear.data.ProtectedSession
import com.alorma.contactnotes.wear.data.ProtectedSessions
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Estat de l'aprovació d'una persona protegida: pantalles 7, 10, 11 i 12 del disseny. */
sealed interface ApprovalUiState {
  /** Pantalla 7: espera que l'usuari confirmi al mòbil. [secondsLeft] només és el compte enrere dibuixat; el temps el valida el mòbil. */
  data class Waiting(
    val secondsLeft: Int,
    val totalSeconds: Int,
  ) : ApprovalUiState

  /** Aprovada: les entrades ja són a [ProtectedSessions] i la pantalla passa a la cronologia. */
  data object Approved : ApprovalUiState

  /** Pantalla 10: l'usuari ha denegat al mòbil, o el mòbil no vol la petició. */
  data object Denied : ApprovalUiState

  /** Pantalla 11: ningú ha confirmat a temps. */
  data object Expired : ApprovalUiState

  /** Pantalla 12: no hi ha mòbil o no ha respost. */
  data object NoPhone : ApprovalUiState
}

class ApprovalViewModel(
  private val personId: Long,
  private val client: ApprovalClient,
  private val results: Flow<WearApprovalResult>,
  private val sessions: ProtectedSessions,
  private val newId: () -> String = { UUID.randomUUID().toString() },
  private val ttlMillis: Long = WEAR_APPROVAL_TTL_MS,
) : ViewModel() {
  private val totalSeconds = (ttlMillis / 1_000).toInt()
  private val _state = MutableStateFlow<ApprovalUiState>(ApprovalUiState.Waiting(totalSeconds, totalSeconds))
  val state: StateFlow<ApprovalUiState> = _state.asStateFlow()

  private var attempt: Job? = null
  private var requestId: String? = null

  init {
    start()
  }

  /** «Reintenta»: una petició nova, amb un identificador nou. */
  fun retry() = start()

  /** «Cancel·la»: el mòbil retira la notificació i la petició queda invalidada. */
  fun cancel() {
    stop()
  }

  override fun onCleared() {
    // Sortir de la pantalla sense que s'hagi resolt és el mateix que cancel·lar
    if (_state.value is ApprovalUiState.Waiting) stop()
  }

  private fun stop() {
    attempt?.cancel()
    requestId?.let(client::cancel)
    requestId = null
  }

  private fun start() {
    attempt?.cancel()
    val id = newId()
    requestId = id
    _state.value = ApprovalUiState.Waiting(totalSeconds, totalSeconds)
    attempt =
      viewModelScope.launch {
        // L'escolta comença abans d'enviar la petició perquè cap resultat es pugui perdre
        val answer =
          async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(ttlMillis) { results.first { it.requestId == id } }
          }
        when (client.request(id, personId)) {
          ApprovalAckResult.Unavailable -> {
            answer.cancel()
            finish(ApprovalUiState.NoPhone)
          }
          ApprovalAckResult.Rejected -> {
            answer.cancel()
            finish(ApprovalUiState.Denied)
          }
          ApprovalAckResult.Pending -> {
            val countdown = launch { tick() }
            val result = answer.await()
            countdown.cancel()
            // Sense resposta a temps, el mòbil encara pot tenir la notificació: se li diu que la retiri
            if (result == null) client.cancel(id)
            finish(result?.let(::resolve) ?: ApprovalUiState.Expired)
          }
        }
      }
  }

  private suspend fun tick() {
    for (left in totalSeconds - 1 downTo 0) {
      delay(1_000)
      _state.value = ApprovalUiState.Waiting(left, totalSeconds)
    }
  }

  private fun resolve(result: WearApprovalResult): ApprovalUiState =
    when (result.decision) {
      WearApprovalDecision.APPROVED -> {
        sessions.open(
          ProtectedSession(personId, result.entries, result.hasMore, result.noteToken, result.noteTokenExpiresAt),
        )
        ApprovalUiState.Approved
      }
      WearApprovalDecision.DENIED -> ApprovalUiState.Denied
      WearApprovalDecision.EXPIRED -> ApprovalUiState.Expired
    }

  private fun finish(next: ApprovalUiState) {
    // La petició ja no és viva: res a cancel·lar en sortir
    requestId = null
    _state.value = next
  }
}

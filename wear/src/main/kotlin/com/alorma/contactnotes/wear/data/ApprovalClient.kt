package com.alorma.contactnotes.wear.data

import android.content.Context
import com.alorma.contactnotes.shared.WEAR_APPROVAL_CANCEL_PATH
import com.alorma.contactnotes.shared.WEAR_APPROVAL_PATH
import com.alorma.contactnotes.shared.WEAR_APPROVAL_RESULT_PATH
import com.alorma.contactnotes.shared.WearApprovalAck
import com.alorma.contactnotes.shared.WearApprovalCancel
import com.alorma.contactnotes.shared.WearApprovalCodec
import com.alorma.contactnotes.shared.WearApprovalRequest
import com.alorma.contactnotes.shared.WearApprovalResult
import com.alorma.contactnotes.shared.WearApprovalStatus
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Què ha contestat el mòbil a una petició d'aprovació. */
enum class ApprovalAckResult {
  /** El mòbil l'ha rebuda i espera l'aprovació de l'usuari; el resultat arriba per [ApprovalResults]. */
  Pending,

  /** El mòbil no vol res d'això (persona inexistent, arxivada o no protegida, petició repetida o massa freqüent). */
  Rejected,

  /** No hi ha mòbil o no ha respost a temps: pantalla «No disponible sense el mòbil». */
  Unavailable,
}

/** Demana al mòbil l'aprovació d'una persona protegida. És una interfície perquè el ViewModel es pugui provar. */
interface ApprovalClient {
  suspend fun request(
    requestId: String,
    personId: Long,
  ): ApprovalAckResult

  /** Avisa el mòbil que l'usuari ja no vol la petició (toc a «Cancel·la» o sortir de la pantalla). No espera resposta. */
  fun cancel(requestId: String)
}

/**
 * Resultats d'aprovació que arriben del mòbil. Només viuen en memòria i no tenen repetició: qui no n'escolta en aquell
 * moment, el perd, i la petició caduca pel seu compte.
 */
object ApprovalResults {
  private val _results = MutableSharedFlow<WearApprovalResult>(extraBufferCapacity = 8)
  val results: Flow<WearApprovalResult> = _results.asSharedFlow()

  fun emit(result: WearApprovalResult) {
    _results.tryEmit(result)
  }
}

class GmsApprovalClient(
  context: Context,
  private val timeoutMillis: Long = TIMEOUT_MILLIS,
) : ApprovalClient {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override suspend fun request(
    requestId: String,
    personId: Long,
  ): ApprovalAckResult =
    try {
      val nodeId = nearbyPhoneNodeId(appContext) ?: return ApprovalAckResult.Unavailable
      val payload =
        WearApprovalCodec
          .encodeRequest(WearApprovalRequest(requestId = requestId, personId = personId))
          .toByteArray(Charsets.UTF_8)
      val reply =
        withTimeout(timeoutMillis) {
          Wearable.getMessageClient(appContext).sendRequest(nodeId, WEAR_APPROVAL_PATH, payload).await()
        }
      val ack: WearApprovalAck? = WearApprovalCodec.decodeAck(reply.toString(Charsets.UTF_8))
      when {
        ack == null || ack.requestId != requestId -> ApprovalAckResult.Unavailable
        ack.status == WearApprovalStatus.PENDING -> ApprovalAckResult.Pending
        else -> ApprovalAckResult.Rejected
      }
    } catch (_: ApiException) {
      ApprovalAckResult.Unavailable
    } catch (_: TimeoutCancellationException) {
      ApprovalAckResult.Unavailable
    }

  override fun cancel(requestId: String) {
    scope.launch {
      try {
        val nodeId = nearbyPhoneNodeId(appContext) ?: return@launch
        val payload =
          WearApprovalCodec.encodeCancel(WearApprovalCancel(requestId = requestId)).toByteArray(Charsets.UTF_8)
        Wearable.getMessageClient(appContext).sendMessage(nodeId, WEAR_APPROVAL_CANCEL_PATH, payload).await()
      } catch (_: ApiException) {
        // Sense mòbil la petició caduca sola
      }
    }
  }

  private companion object {
    const val TIMEOUT_MILLIS = 8_000L
  }
}

/** Rep el resultat de l'aprovació que envia el mòbil i el passa a [ApprovalResults]. No registra mai el contingut. */
class ApprovalResultListenerService : WearableListenerService() {
  override fun onMessageReceived(messageEvent: MessageEvent) {
    if (messageEvent.path != WEAR_APPROVAL_RESULT_PATH) return
    WearApprovalCodec.decodeResult(messageEvent.data.toString(Charsets.UTF_8))?.let(ApprovalResults::emit)
  }
}

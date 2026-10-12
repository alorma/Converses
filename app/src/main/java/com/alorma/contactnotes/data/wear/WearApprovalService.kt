package com.alorma.contactnotes.data.wear

import android.content.Context
import com.alorma.contactnotes.shared.WEAR_APPROVAL_CANCEL_PATH
import com.alorma.contactnotes.shared.WEAR_APPROVAL_PATH
import com.alorma.contactnotes.shared.WEAR_APPROVAL_RESULT_PATH
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject

/**
 * Rep les peticions d'aprovació d'una persona protegida (`MessageClient.sendRequest`, amb un ack immediat) i les
 * cancel·lacions. Només el rellotge emparellat pot parlar-hi: la Data Layer no entrega missatges d'altres dispositius.
 */
class WearApprovalService : WearableListenerService() {
  private val coordinator: WearApprovalCoordinator by inject()

  override fun onRequest(
    nodeId: String,
    path: String,
    request: ByteArray,
  ): Task<ByteArray>? {
    if (path != WEAR_APPROVAL_PATH) return null
    // L'ack ha d'estar llest abans de tornar; la notificació surt dins de la mateixa crida
    val ack = runBlocking { coordinator.onRequest(nodeId, request.toString(Charsets.UTF_8)) } ?: return null
    return Tasks.forResult(ack.toByteArray(Charsets.UTF_8))
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    if (messageEvent.path == WEAR_APPROVAL_CANCEL_PATH) {
      coordinator.onCancel(messageEvent.data.toString(Charsets.UTF_8))
    }
  }
}

/** Envia el resultat de l'aprovació al rellotge per `MessageClient`. Si el rellotge no hi és, es perd: caldrà una petició nova. */
class GmsApprovalResultSender(
  private val context: Context,
) : ApprovalResultSender {
  override suspend fun send(
    nodeId: String,
    payload: String,
  ) {
    try {
      Wearable
        .getMessageClient(context.applicationContext)
        .sendMessage(nodeId, WEAR_APPROVAL_RESULT_PATH, payload.toByteArray(Charsets.UTF_8))
        .await()
    } catch (_: ApiException) {
      // El rellotge ja no és accessible: mostrarà «Ha caducat» pel seu compte enrere
    }
  }
}

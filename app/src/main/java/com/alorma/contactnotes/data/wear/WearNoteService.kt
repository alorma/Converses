package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.WEAR_NOTE_PATH
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/** Rep les notes dictades al rellotge (`MessageClient.sendRequest`) i en confirma l'entrega. */
class WearNoteService : WearableListenerService() {
  private val receiver: WearNoteReceiver by inject()

  override fun onRequest(
    nodeId: String,
    path: String,
    request: ByteArray,
  ): Task<ByteArray>? {
    if (path != WEAR_NOTE_PATH) return null
    // La resposta ha d'estar llesta abans de tornar; el rellotge reintenta si no hi és
    val response = runBlocking { receiver.receive(request.toString(Charsets.UTF_8)) } ?: return null
    return Tasks.forResult(response.toByteArray(Charsets.UTF_8))
  }
}

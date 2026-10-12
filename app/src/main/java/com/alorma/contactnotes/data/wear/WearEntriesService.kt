package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.WEAR_ENTRIES_PATH
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/** Rep les peticions de «més entrades» del rellotge (`MessageClient.sendRequest`) i en retorna la pàgina. */
class WearEntriesService : WearableListenerService() {
  private val responder: WearEntriesResponder by inject()

  override fun onRequest(
    nodeId: String,
    path: String,
    request: ByteArray,
  ): Task<ByteArray>? {
    if (path != WEAR_ENTRIES_PATH) return null
    // El servei és de vida curta i la consulta és petita; la resposta ha d'estar llesta abans de tornar
    val response = runBlocking { responder.respond(request.toString(Charsets.UTF_8)) } ?: return null
    return Tasks.forResult(response.toByteArray(Charsets.UTF_8))
  }
}

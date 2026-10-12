package com.alorma.contactnotes.data.wear

import android.content.Context
import android.net.Uri
import com.alorma.contactnotes.shared.WEAR_SNAPSHOT_PATH
import com.alorma.contactnotes.shared.WEAR_WATCH_CAPABILITY
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Canal cap al rellotge. És una interfície perquè el publicador es pugui provar sense Play Services. */
interface WearLink {
  /**
   * Emet `true` quan hi ha un rellotge emparellat amb l'app instal·lada (encara que ara no sigui a prop) i `false`
   * quan no n'hi ha cap. Sense rellotge no es publica res.
   */
  val watchPresent: Flow<Boolean>

  /** Desa l'instantani a la Data Layer; només viatja cap al rellotge emparellat. */
  suspend fun publish(payload: ByteArray)

  /** Esborra l'instantani de la Data Layer (el rellotge ja no hi és). */
  suspend fun clear()
}

class GmsWearLink(
  context: Context,
) : WearLink {
  private val appContext = context.applicationContext

  override val watchPresent: Flow<Boolean> =
    callbackFlow {
      val client = Wearable.getCapabilityClient(appContext)
      val listener =
        CapabilityClient.OnCapabilityChangedListener { _: CapabilityInfo ->
          launch { send(isWatchPresent()) }
        }
      try {
        send(isWatchPresent())
        client.addListener(listener, WEAR_WATCH_CAPABILITY).await()
      } catch (_: ApiException) {
        // Sense Play Services o sense l'API de Wear: es tracta com a "cap rellotge"
        trySend(false)
      }
      awaitClose { client.removeListener(listener) }
    }

  /**
   * Hi ha rellotge si algun node (encara que ara no sigui a prop) té l'app, o si hi ha algun node connectat. El segon
   * cas cobreix els entorns on les capacitats no es propaguen (p. ex. els emuladors). Els canvis de capacitat són els
   * únics esdeveniments que reavaluen l'estat mentre l'app és oberta: Play Services no dona cap *listener* de nodes.
   */
  private suspend fun isWatchPresent(): Boolean =
    try {
      val withApp =
        Wearable
          .getCapabilityClient(
            appContext,
          ).getCapability(WEAR_WATCH_CAPABILITY, CapabilityClient.FILTER_ALL)
          .await()
      withApp.nodes.isNotEmpty() ||
        Wearable
          .getNodeClient(appContext)
          .connectedNodes
          .await()
          .isNotEmpty()
    } catch (_: ApiException) {
      false
    }

  override suspend fun publish(payload: ByteArray) {
    val request = PutDataRequest.create(WEAR_SNAPSHOT_PATH).setData(payload).setUrgent()
    Wearable.getDataClient(appContext).putDataItem(request).await()
  }

  override suspend fun clear() {
    val uri =
      Uri
        .Builder()
        .scheme("wear")
        .path(WEAR_SNAPSHOT_PATH)
        .build()
    Wearable.getDataClient(appContext).deleteDataItems(uri).await()
  }
}

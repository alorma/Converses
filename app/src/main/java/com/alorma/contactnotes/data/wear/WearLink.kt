package com.alorma.contactnotes.data.wear

import android.content.Context
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
import kotlinx.coroutines.tasks.await

/** Canal cap al rellotge. És una interfície perquè el publicador es pugui provar sense Play Services. */
interface WearLink {
  /** Emet `true` quan hi ha un rellotge amb l'app accessible, i `false` quan deixa de ser-ho. */
  val watchAvailable: Flow<Boolean>

  /** Desa l'instantani a la Data Layer; només viatja cap al rellotge emparellat. */
  suspend fun publish(payload: ByteArray)
}

class GmsWearLink(
  context: Context,
) : WearLink {
  private val appContext = context.applicationContext

  override val watchAvailable: Flow<Boolean> =
    callbackFlow {
      val client = Wearable.getCapabilityClient(appContext)
      val listener =
        CapabilityClient.OnCapabilityChangedListener { info: CapabilityInfo ->
          trySend(info.nodes.isNotEmpty())
        }
      try {
        val initial = client.getCapability(WEAR_WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await()
        trySend(initial.nodes.isNotEmpty())
        client.addListener(listener, WEAR_WATCH_CAPABILITY).await()
      } catch (_: ApiException) {
        // Sense Play Services o sense l'API de Wear: es tracta com a "cap rellotge"
        trySend(false)
      }
      awaitClose { client.removeListener(listener) }
    }

  override suspend fun publish(payload: ByteArray) {
    val request = PutDataRequest.create(WEAR_SNAPSHOT_PATH).setData(payload).setUrgent()
    Wearable.getDataClient(appContext).putDataItem(request).await()
  }
}

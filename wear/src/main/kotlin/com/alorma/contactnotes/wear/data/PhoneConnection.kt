package com.alorma.contactnotes.wear.data

import android.content.Context
import com.alorma.contactnotes.shared.WEAR_PHONE_CAPABILITY
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Si el mòbil és accessible ara. És una interfície perquè els ViewModels es puguin provar sense Play Services. */
interface PhoneConnection {
  /** `true` quan el mòbil és a prop; `false` en qualsevol altre cas (també sense Play Services). */
  val connected: Flow<Boolean>
}

class GmsPhoneConnection(
  context: Context,
) : PhoneConnection {
  private val appContext = context.applicationContext

  override val connected: Flow<Boolean> =
    callbackFlow {
      val client = Wearable.getCapabilityClient(appContext)
      val listener =
        CapabilityClient.OnCapabilityChangedListener { _: CapabilityInfo ->
          launch { send(isPhoneNearby()) }
        }
      try {
        send(isPhoneNearby())
        client.addListener(listener, WEAR_PHONE_CAPABILITY).await()
      } catch (_: ApiException) {
        // Sense Play Services o sense l'API de Wear: es tracta com a "mòbil no connectat"
        trySend(false)
      }
      awaitClose { client.removeListener(listener) }
    }.distinctUntilChanged()

  /**
   * El mòbil és a prop si algun node amb l'app (capacitat `converses_phone`) o, si les capacitats no es propaguen
   * (p. ex. als emuladors), algun node connectat hi és. El rellotge només parla amb el seu mòbil emparellat.
   */
  private suspend fun isPhoneNearby(): Boolean =
    try {
      val withApp =
        Wearable
          .getCapabilityClient(appContext)
          .getCapability(WEAR_PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
          .await()
      withApp.nodes.any { it.isNearby } ||
        Wearable
          .getNodeClient(appContext)
          .connectedNodes
          .await()
          .any { it.isNearby }
    } catch (_: ApiException) {
      false
    }
}

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
import kotlinx.coroutines.tasks.await

/** Si el mòbil és accessible ara. És una interfície perquè els ViewModels es puguin provar sense Play Services. */
interface PhoneConnection {
  /** `true` quan el mòbil és a prop i té l'app; `false` en qualsevol altre cas (també sense Play Services). */
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
        CapabilityClient.OnCapabilityChangedListener { info: CapabilityInfo ->
          trySend(info.isPhoneNearby())
        }
      try {
        client.addListener(listener, WEAR_PHONE_CAPABILITY).await()
        send(client.getCapability(WEAR_PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await().isPhoneNearby())
      } catch (_: ApiException) {
        trySend(false)
      }
      awaitClose { client.removeListener(listener) }
    }.distinctUntilChanged()

  private fun CapabilityInfo.isPhoneNearby(): Boolean = nodes.any { it.isNearby }
}

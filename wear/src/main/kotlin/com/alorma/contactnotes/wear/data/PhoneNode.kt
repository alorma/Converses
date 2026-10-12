package com.alorma.contactnotes.wear.data

import android.content.Context
import com.alorma.contactnotes.shared.WEAR_PHONE_CAPABILITY
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** El mòbil a prop: node amb l'app o, si les capacitats no es propaguen (emuladors), qualsevol node connectat. */
internal suspend fun nearbyPhoneNodeId(context: Context): String? {
  val appContext = context.applicationContext
  val withApp =
    Wearable
      .getCapabilityClient(appContext)
      .getCapability(WEAR_PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
      .await()
  withApp.nodes.firstOrNull { it.isNearby }?.let { return it.id }
  return Wearable
    .getNodeClient(appContext)
    .connectedNodes
    .await()
    .firstOrNull { it.isNearby }
    ?.id
}

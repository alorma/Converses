package com.alorma.contactnotes.wear.data

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import com.alorma.contactnotes.shared.WEAR_PHONE_CAPABILITY
import com.alorma.contactnotes.shared.companionStateOf
import com.alorma.contactnotes.shared.playStoreUri
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Si l'app és al mòbil emparellat, i com demanar d'instal·lar-la. És una interfície perquè es pugui provar. */
interface PhoneInstall {
  /** L'estat del mòbil; sense Play Services és [CompanionState.NoDevice]. */
  val state: Flow<CompanionState>

  /** Obre la fitxa de Google Play al mòbil. `false` si el sistema no ho ha pogut fer. */
  suspend fun openPlayOnPhone(node: CompanionNode): Boolean
}

class GmsPhoneInstall(
  context: Context,
) : PhoneInstall {
  private val appContext = context.applicationContext

  override val state: Flow<CompanionState> =
    callbackFlow {
      val client = Wearable.getCapabilityClient(appContext)
      val listener =
        CapabilityClient.OnCapabilityChangedListener { _: CapabilityInfo ->
          launch { send(currentState()) }
        }
      try {
        send(currentState())
        client.addListener(listener, WEAR_PHONE_CAPABILITY).await()
      } catch (_: ApiException) {
        trySend(CompanionState.NoDevice)
      }
      awaitClose { client.removeListener(listener) }
    }.distinctUntilChanged()

  private suspend fun currentState(): CompanionState =
    try {
      val withApp =
        Wearable
          .getCapabilityClient(appContext)
          .getCapability(WEAR_PHONE_CAPABILITY, CapabilityClient.FILTER_ALL)
          .await()
          .nodes
      val connected = Wearable.getNodeClient(appContext).connectedNodes.await()
      companionStateOf(withApp.map { it.toCompanion() }, connected.map { it.toCompanion() })
    } catch (_: ApiException) {
      CompanionState.NoDevice
    }

  override suspend fun openPlayOnPhone(node: CompanionNode): Boolean =
    try {
      val intent =
        Intent(Intent.ACTION_VIEW)
          .addCategory(Intent.CATEGORY_BROWSABLE)
          .setData(playStoreUri(appContext.packageName).toUri())
      RemoteActivityHelper(appContext, Dispatchers.IO.asExecutor()).startRemoteActivity(intent, node.id).await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // El sistema pot refusar l'obertura remota (RemoteActivityHelper.RemoteIntentException o Play Services)
      false
    }
}

private fun Node.toCompanion() = CompanionNode(id = id, displayName = displayName, isNearby = isNearby)

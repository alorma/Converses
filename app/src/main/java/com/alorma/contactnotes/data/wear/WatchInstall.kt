package com.alorma.contactnotes.data.wear

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import com.alorma.contactnotes.shared.WEAR_WATCH_CAPABILITY
import com.alorma.contactnotes.shared.companionStateOf
import com.alorma.contactnotes.shared.playStoreUri
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await

/** Si l'app és al rellotge emparellat, i com proposar d'instal·lar-la. És una interfície perquè es pugui provar. */
interface WatchInstall {
  /** L'estat del rellotge; sense Play Services és [CompanionState.NoDevice]. */
  suspend fun state(): CompanionState

  /** Obre la fitxa de Google Play al rellotge. `false` si el sistema no ho ha pogut fer. */
  suspend fun openPlayOnWatch(node: CompanionNode): Boolean
}

class GmsWatchInstall(
  context: Context,
) : WatchInstall {
  private val appContext = context.applicationContext

  override suspend fun state(): CompanionState =
    try {
      val withApp =
        Wearable
          .getCapabilityClient(appContext)
          .getCapability(WEAR_WATCH_CAPABILITY, CapabilityClient.FILTER_ALL)
          .await()
          .nodes
      val connected = Wearable.getNodeClient(appContext).connectedNodes.await()
      companionStateOf(withApp.map { it.toCompanion() }, connected.map { it.toCompanion() })
    } catch (_: ApiException) {
      CompanionState.NoDevice
    }

  override suspend fun openPlayOnWatch(node: CompanionNode): Boolean =
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

package com.alorma.contactnotes.data.wear

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** Nom amb què el sistema coneix el rellotge, per dir-ho a l'usuari. És una interfície perquè es pugui provar. */
fun interface WatchNames {
  /** `null` si no es pot saber (rellotge no connectat o sense Play Services). */
  suspend fun nameOf(nodeId: String): String?
}

class GmsWatchNames(
  context: Context,
) : WatchNames {
  private val appContext = context.applicationContext

  override suspend fun nameOf(nodeId: String): String? =
    try {
      Wearable
        .getNodeClient(appContext)
        .connectedNodes
        .await()
        .firstOrNull { it.id == nodeId }
        ?.displayName
        ?.takeIf { it.isNotBlank() }
    } catch (_: ApiException) {
      null
    }
}

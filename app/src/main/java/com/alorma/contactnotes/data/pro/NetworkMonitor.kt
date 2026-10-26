package com.alorma.contactnotes.data.pro

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Si hi ha connexió: sense, no es pot comprar ni restaurar (disseny, «Estats de la compra»). */
interface NetworkMonitor {
  val isOnline: Flow<Boolean>
}

class AndroidNetworkMonitor(
  context: Context,
) : NetworkMonitor {
  private val connectivity = context.getSystemService(ConnectivityManager::class.java)

  override val isOnline: Flow<Boolean> =
    callbackFlow {
      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities,
          ) {
            trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
          }

          override fun onLost(network: Network) {
            trySend(false)
          }
        }
      trySend(connectivity.isOnlineNow())
      connectivity.registerDefaultNetworkCallback(callback)
      awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

  private fun ConnectivityManager.isOnlineNow(): Boolean =
    getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}

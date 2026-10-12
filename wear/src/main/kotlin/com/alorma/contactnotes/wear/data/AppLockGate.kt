package com.alorma.contactnotes.wear.data

import android.app.KeyguardManager
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

/** Si el rellotge té un bloqueig de pantalla propi. És una interfície perquè es pugui provar sense Android. */
fun interface DeviceLock {
  fun isSecure(): Boolean
}

class AndroidDeviceLock(
  context: Context,
) : DeviceLock {
  private val keyguard = context.applicationContext.getSystemService(KeyguardManager::class.java)

  // Si el sistema no ho sap dir, es tracta com a no segur
  override fun isSecure(): Boolean = keyguard?.isDeviceSecure ?: false
}

/**
 * Cal el bloqueig del rellotge quan el Bloqueig de l'app és actiu al mòbil i el rellotge no és segur. Si no es pot
 * verificar l'estat ([appLockActive] és `null`, perquè encara no hi ha cap instantani), s'assumeix **actiu**.
 */
fun isLockRequired(
  appLockActive: Boolean?,
  deviceSecure: Boolean,
): Boolean = (appLockActive ?: true) && !deviceSecure

/**
 * Decideix si el rellotge ha de mostrar la pantalla «Cal un bloqueig» i, en aquest cas, **esborra la còpia local**
 * (en obrir l'app, en tornar-hi i a cada instantani nou). [recheck] s'ha de cridar quan l'app torna a primer pla, perquè
 * l'usuari pot haver tret el bloqueig mentre era fora.
 */
class AppLockGate(
  snapshots: Flow<StoredSnapshot?>,
  private val deviceLock: DeviceLock,
  private val purge: suspend () -> Unit,
) {
  private val ticks = MutableStateFlow(0)

  val lockRequired: Flow<Boolean> =
    combine(snapshots, ticks) { stored, _ ->
      val required = isLockRequired(stored?.snapshot?.appLockActive, deviceLock.isSecure())
      if (required && stored != null) purge()
      required
    }.distinctUntilChanged()

  fun recheck() {
    ticks.update { it + 1 }
  }
}

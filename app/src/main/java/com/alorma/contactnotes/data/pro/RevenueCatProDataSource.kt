package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** L'entitlement de RevenueCat que fa Pro. El codi el consulta sempre a ell, mai el producte (`lifetime`). */
const val PRO_ENTITLEMENT = "converses_pro"

/** L'SDK de RevenueCat, darrere d'una interfície perquè els tests de JVM no el toquin. */
interface PurchasesGateway {
  /** Configura l'SDK i avisa, a cada `CustomerInfo` que arriba, de si [PRO_ENTITLEMENT] és actiu. */
  fun start(onEntitlement: (Boolean) -> Unit)

  /** Preu del paquet de l'offering actual (`default`, amb `lifetime`); `null` si no es pot obtenir. */
  suspend fun price(): String?

  suspend fun purchase(activity: Activity): PurchaseResult

  suspend fun restore(): RestoreResult
}

/**
 * Pro segons RevenueCat. Arrenca amb l'últim valor conegut ([initial], desat amb [persist]): l'SDK es configura després
 * del primer frame i sense connexió pot no respondre, i un usuari Pro no ha de veure les funcions bloquejades mentrestant.
 */
class RevenueCatProDataSource(
  private val gateway: PurchasesGateway,
  initial: Boolean = false,
  private val persist: (Boolean) -> Unit = {},
  /**
   * Pro canvia pel listener de RevenueCat i no per una compra o restauració d'aquesta pantalla: un pagament pendent
   * que es confirma, un reemborsament o una compra feta en un altre mòbil.
   */
  private val onExternalChange: (Boolean) -> Unit = {},
) : ProDataSource {
  private val state = MutableStateFlow(initial)

  override val isPro: StateFlow<Boolean> = state.asStateFlow()

  override fun start() {
    gateway.start { active -> if (update(active)) onExternalChange(active) }
  }

  override suspend fun price(): String? = gateway.price()

  // Pro s'encén de seguida, sense esperar el listener de `CustomerInfo`
  override suspend fun purchase(activity: Activity): PurchaseResult =
    gateway.purchase(activity).also { if (it == PurchaseResult.Purchased) update(true) }

  override suspend fun restore(): RestoreResult =
    gateway.restore().also { if (it == RestoreResult.Restored) update(true) }

  /** Si ha canviat res. */
  private fun update(active: Boolean): Boolean {
    if (state.value == active) return false
    state.value = active
    persist(active)
    return true
  }
}

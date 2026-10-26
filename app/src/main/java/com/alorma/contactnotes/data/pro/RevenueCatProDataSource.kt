package com.alorma.contactnotes.data.pro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** L'entitlement de RevenueCat que fa Pro. El codi el consulta sempre a ell, mai el producte (`lifetime`). */
const val PRO_ENTITLEMENT = "converses_pro"

/** L'SDK de RevenueCat, darrere d'una interfície perquè els tests de JVM no el toquin. */
interface PurchasesGateway {
  /** Configura l'SDK i avisa, a cada `CustomerInfo` que arriba, de si [PRO_ENTITLEMENT] és actiu. */
  fun start(onEntitlement: (Boolean) -> Unit)
}

/**
 * Pro segons RevenueCat. Arrenca amb l'últim valor conegut ([initial], desat amb [persist]): l'SDK es configura després
 * del primer frame i sense connexió pot no respondre, i un usuari Pro no ha de veure les funcions bloquejades mentrestant.
 */
class RevenueCatProDataSource(
  private val gateway: PurchasesGateway,
  initial: Boolean = false,
  private val persist: (Boolean) -> Unit = {},
) : ProDataSource {
  private val state = MutableStateFlow(initial)

  override val isPro: StateFlow<Boolean> = state.asStateFlow()

  override fun start() {
    gateway.start { active ->
      if (state.value != active) {
        state.value = active
        persist(active)
      }
    }
  }
}

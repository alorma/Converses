package com.alorma.contactnotes.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Punt únic per decidir si les funcions Pro són visibles. A la v1 sempre és `false` i no es comprova cap pagament;
 * quan hi hagi Pro, aquí és on es mirarà si l'usuari ha pagat. La UI mai no ensenya res de gratuït/Pro.
 */
class FeatureGate {
  private val enabled = MutableStateFlow(false)

  val proEnabled: StateFlow<Boolean> = enabled.asStateFlow()

  /** Només el menú de debug el crida, per provar els dos modes. */
  fun setProEnabled(value: Boolean) {
    enabled.value = value
  }
}

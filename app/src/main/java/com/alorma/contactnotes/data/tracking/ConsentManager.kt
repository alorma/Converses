package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * El consentiment de les estadístiques d'ús. [consent] és `null` mentre l'usuari no ha triat, i en aquest cas no
 * s'envia res, igual que amb «No».
 *
 * El valor inicial es llegeix de manera síncrona perquè els esdeveniments de l'arrencada no es perdin mentre el
 * DataStore carrega (el mateix DataStore ja s'ha llegit per l'onboarding).
 */
class ConsentManager(
  private val settings: SettingsRepository,
  private val scope: CoroutineScope,
) {
  val consent: StateFlow<Boolean?> =
    settings.settings
      .map { it.analyticsConsent }
      .stateIn(scope, SharingStarted.Eagerly, runBlocking { settings.settings.first().analyticsConsent })

  /** Si es pot enviar res fora del mòbil ara mateix. */
  fun hasConsent(): Boolean = consent.value == true

  fun setConsent(granted: Boolean) {
    scope.launch { saveConsent(granted) }
  }

  /** Torna a deixar el consentiment sense triar, perquè el full torni a sortir. Només des del menú de debug. */
  fun clearConsent() {
    scope.launch { saveConsent(null) }
  }

  /**
   * Desa la tria i no torna fins que [consent] ja la té, perquè el que es registri just després (per exemple, el
   * canvi d'Ajustos) ja vagi amb el consentiment nou. `null` l'esborra (només des del menú de debug).
   */
  suspend fun saveConsent(granted: Boolean?) {
    settings.setAnalyticsConsent(granted)
    consent.first { it == granted }
  }
}

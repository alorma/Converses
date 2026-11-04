package com.alorma.contactnotes.data.tracking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Engega i atura Firebase segons el consentiment:
 * - «Sí»: l'inicialitza i n'activa la recollida, a l'arrencada o en el moment de dir-ho.
 * - «No» o sense triar: si ja estava inicialitzat (algú ha dit «Sí» i després «No», o el menú de debug ha esborrat
 *   la tria), n'apaga la recollida i descarta els pendents. Si no, no fa res: l'arrencada següent ja no
 *   l'inicialitza.
 *
 * [FirebaseTracker] també crida [FirebaseGateway.enable] abans d'enviar, perquè la cua de l'[EventTracker] es pot
 * buidar abans que aquest collect reaccioni al «Sí». Per això [FirebaseGateway.enable] es pot cridar més d'un cop.
 */
class FirebaseController(
  private val gateway: FirebaseGateway,
  private val consent: StateFlow<Boolean?>,
  private val scope: CoroutineScope,
) {
  /**
   * Es crida un cop en arrencar l'app (`ConversesApp`). Amb el consentiment ja donat, Firebase s'inicialitza abans
   * de tornar, perquè Crashlytics reculli els errors de l'arrencada.
   */
  fun start() {
    apply(consent.value)
    // Sense drop(1): si el valor canvia abans que el collect comenci, el primer valor ja és el nou
    scope.launch { consent.collect(::apply) }
  }

  private fun apply(granted: Boolean?) {
    if (granted == true) gateway.enable() else gateway.disable()
  }
}

package com.alorma.contactnotes.data.tracking

/**
 * L'única interfície de tracking que coneix l'app. Les implementacions (Timber a debug, Firebase a release) no se
 * criden mai directament: tot passa per [EventTracker].
 */
interface Tracker {
  /**
   * Si envia dades fora del mòbil. [EventTracker] només crida els que ho fan quan l'usuari ha dit que sí a les
   * estadístiques; els locals (logcat) reben sempre.
   */
  val isRemote: Boolean

  fun trackScreen(screen: Screen)

  fun trackEvent(event: Event)

  fun trackAction(action: Action)

  /**
   * Un error que no ha petat l'app. [where] és un nom tancat del lloc (`"export_person"`), mai una dada.
   * Les implementacions remotes no han d'enviar el `message` de l'excepció: pot portar dades de l'usuari.
   */
  fun trackError(
    error: Throwable,
    where: String,
  )
}

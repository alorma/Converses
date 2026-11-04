package com.alorma.contactnotes.data.tracking

/**
 * Firebase Analytics i Crashlytics. És remot: [EventTracker] només el crida amb el consentiment donat, i abans
 * d'enviar res s'assegura que Firebase estigui inicialitzat ([FirebaseGateway.enable]), perquè la cua de pendents
 * es pot buidar abans que [FirebaseController] reaccioni al «Sí».
 *
 * - Pantalles: `screen_view` amb `screen_name`, `event_version` i les propietats.
 * - Esdeveniments i accions: un esdeveniment amb el seu nom, `event_version` i les propietats.
 * - Errors: un error no fatal de Crashlytics sense cap `message` ([sanitize]), amb `where` i les propietats com a
 *   *custom keys* d'aquell error.
 */
class FirebaseTracker(
  private val gateway: FirebaseGateway,
  private val sink: FirebaseSink,
) : Tracker {
  override val isRemote: Boolean = true

  override fun trackScreen(screen: Screen) {
    log(SCREEN_VIEW, screen, mapOf(SCREEN_NAME to screen.eventName))
  }

  override fun trackEvent(event: Event) = log(event.eventName, event)

  override fun trackAction(action: Action) = log(action.eventName, action)

  override fun trackError(
    error: Throwable,
    where: String,
    properties: Map<String, String>,
  ) {
    gateway.enable()
    sink.recordError(sanitize(error), properties + (WHERE to where))
  }

  private fun log(
    name: String,
    event: TrackingEvent,
    extra: Map<String, String> = emptyMap(),
  ) {
    gateway.enable()
    sink.logEvent(name, event.eventProperties + extra + (EVENT_VERSION to event.eventVersion.toString()))
  }

  companion object {
    const val SCREEN_VIEW = "screen_view"
    const val SCREEN_NAME = "screen_name"
    const val EVENT_VERSION = "event_version"
    const val WHERE = "where"

    /**
     * Una còpia de [error] sense cap `message`, que pot portar dades de l'usuari: es queda la classe original (com a
     * únic text), el *stack trace* i les causes, netejades igual.
     */
    fun sanitize(error: Throwable): Throwable = sanitize(error, depth = 0)

    private fun sanitize(
      error: Throwable,
      depth: Int,
    ): Throwable {
      val cause = error.cause?.takeIf { it !== error && depth < MAX_CAUSES }?.let { sanitize(it, depth + 1) }
      return SanitizedException(error::class.java.name, cause).apply { stackTrace = error.stackTrace }
    }

    private const val MAX_CAUSES = 10
  }
}

/** Un error d'on s'ha tret el missatge: només en queda el nom de la classe original. */
class SanitizedException(
  val originalClass: String,
  cause: Throwable?,
) : Exception(originalClass, cause)

/** El que [FirebaseTracker] envia a Firebase, darrere una interfície perquè es pugui provar a la JVM. */
interface FirebaseSink {
  fun logEvent(
    name: String,
    params: Map<String, String>,
  )

  fun recordError(
    error: Throwable,
    keys: Map<String, String>,
  )
}

package com.alorma.contactnotes.data.tracking

/**
 * Release: Firebase Analytics i Crashlytics. De moment no fa res; Firebase arriba a la fase 8 de
 * `docs/analytics/README.md`. És remot, així que [EventTracker] només el crida amb consentiment.
 */
class FirebaseTracker : Tracker {
  override val isRemote: Boolean = true

  override fun trackScreen(screen: Screen) = Unit

  override fun trackEvent(event: Event) = Unit

  override fun trackAction(action: Action) = Unit

  override fun trackError(
    error: Throwable,
    where: String,
    properties: Map<String, String>,
  ) = Unit
}

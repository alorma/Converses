package com.alorma.contactnotes.data.tracking

/**
 * El [Tracker] que fa servir l'app: reparteix cada crida entre tots els [trackers]. Els remots només la reben si
 * [hasConsent] és cert en aquell moment; sense consentiment no se'ls crida mai.
 */
class EventTracker(
  private val trackers: List<Tracker>,
  private val hasConsent: () -> Boolean,
) : Tracker {
  override val isRemote: Boolean = false

  override fun trackScreen(screen: Screen) = dispatch { it.trackScreen(screen) }

  override fun trackEvent(event: Event) = dispatch { it.trackEvent(event) }

  override fun trackAction(action: Action) = dispatch { it.trackAction(action) }

  override fun trackError(
    error: Throwable,
    where: String,
  ) = dispatch { it.trackError(error, where) }

  private inline fun dispatch(call: (Tracker) -> Unit) {
    val consent = hasConsent()
    trackers.filter { !it.isRemote || consent }.forEach(call)
  }
}

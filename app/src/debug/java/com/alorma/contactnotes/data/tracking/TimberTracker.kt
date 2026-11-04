package com.alorma.contactnotes.data.tracking

import timber.log.Timber

/** Debug: escriu cada esdeveniment al logcat (etiqueta `Tracking`). No surt del mòbil, així que rep sempre. */
class TimberTracker : Tracker {
  init {
    if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
  }

  override val isRemote: Boolean = false

  override fun trackScreen(screen: Screen) = log("screen", screen)

  override fun trackEvent(event: Event) = log("event", event)

  override fun trackAction(action: Action) = log("action", action)

  override fun trackError(
    error: Throwable,
    where: String,
  ) {
    Timber.tag(TAG).e(error, "error where=%s", where)
  }

  private fun log(
    type: String,
    event: TrackingEvent,
  ) {
    Timber.tag(TAG).d("%s %s v%d %s", type, event.eventName, event.eventVersion, event.eventProperties)
  }

  private companion object {
    const val TAG = "Tracking"
  }
}

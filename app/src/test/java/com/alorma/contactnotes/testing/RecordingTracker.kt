package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.tracking.Action
import com.alorma.contactnotes.data.tracking.Event
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.Screen
import com.alorma.contactnotes.data.tracking.Tracker

/** Guarda tot el que rep. [eventTracker] és el que es passa als ViewModels, com fa Koin. */
class RecordingTracker : Tracker {
  override val isRemote: Boolean = false

  val actions = mutableListOf<Action>()
  val events = mutableListOf<Event>()

  val eventTracker = EventTracker(listOf(this)) { false }

  /** Nom i propietats de cada acció, en ordre. */
  fun recordedActions(): List<Pair<String, Map<String, String>>> = actions.map { it.eventName to it.eventProperties }

  override fun trackScreen(screen: Screen) = Unit

  override fun trackEvent(event: Event) {
    events += event
  }

  override fun trackAction(action: Action) {
    actions += action
  }

  override fun trackError(
    error: Throwable,
    where: String,
  ) = Unit
}

package com.alorma.contactnotes.data.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * El [Tracker] que fa servir l'app: reparteix cada crida entre tots els [trackers]. Els remots només la reben si
 * [hasConsent] és cert en aquell moment; sense consentiment no se'ls crida mai.
 *
 * Les propietats comunes ([setCommonProperty]) s'afegeixen a tot el que s'envia. Si una clau coincideix amb una de
 * l'esdeveniment, guanya la de l'esdeveniment. Mai hi va cap dada de l'usuari: només valors tancats.
 */
class EventTracker(
  private val trackers: List<Tracker>,
  private val hasConsent: () -> Boolean,
) : Tracker {
  override val isRemote: Boolean = false

  private val common = MutableStateFlow<Map<String, String>>(emptyMap())

  fun setCommonProperty(
    key: String,
    value: String,
  ) = common.update { it + (key to value) }

  fun removeCommonProperty(key: String) = common.update { it - key }

  override fun trackScreen(screen: Screen) {
    val merged = merge(screen) { CommonScreen(screen, it) }
    dispatch { it.trackScreen(merged) }
  }

  override fun trackEvent(event: Event) {
    val merged = merge(event) { CommonEvent(event, it) }
    dispatch { it.trackEvent(merged) }
  }

  override fun trackAction(action: Action) {
    val merged = merge(action) { CommonAction(action, it) }
    dispatch { it.trackAction(merged) }
  }

  override fun trackError(
    error: Throwable,
    where: String,
    properties: Map<String, String>,
  ) {
    val merged = common.value + properties
    dispatch { it.trackError(error, where, merged) }
  }

  private inline fun <T : TrackingEvent> merge(
    event: T,
    copy: (Map<String, String>) -> T,
  ): T {
    val properties = common.value
    return if (properties.isEmpty()) event else copy(properties + event.eventProperties)
  }

  private inline fun dispatch(call: (Tracker) -> Unit) {
    val consent = hasConsent()
    trackers.filter { !it.isRemote || consent }.forEach(call)
  }

  private class CommonScreen(
    screen: Screen,
    properties: Map<String, String>,
  ) : Screen(screen.eventName, screen.eventVersion, properties)

  private class CommonEvent(
    event: Event,
    properties: Map<String, String>,
  ) : Event(event.eventName, event.eventVersion, properties)

  private class CommonAction(
    action: Action,
    properties: Map<String, String>,
  ) : Action(action.eventName, action.eventVersion, properties)
}

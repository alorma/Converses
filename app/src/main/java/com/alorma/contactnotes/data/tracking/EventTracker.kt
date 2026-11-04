package com.alorma.contactnotes.data.tracking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El [Tracker] que fa servir l'app: reparteix cada crida entre tots els [trackers]. Els locals la reben sempre; els
 * remots depenen de [consent]:
 * - `true`: la reben a l'instant.
 * - `false`: no se'ls crida mai.
 * - `null` (l'usuari encara no ha triat): es guarda en una cua en memòria, que s'envia en ordre si tria «Sí» i es
 *   buida si tria «No». No es desa mai al disc: si l'app es tanca, es perd. Té un límit de [MAX_PENDING]; en
 *   passar-lo es descarten els més antics.
 *
 * Les propietats comunes ([setCommonProperty]) s'afegeixen a tot el que s'envia. Si una clau coincideix amb una de
 * l'esdeveniment, guanya la de l'esdeveniment. Mai hi va cap dada de l'usuari: només valors tancats.
 */
class EventTracker(
  private val trackers: List<Tracker>,
  private val consent: StateFlow<Boolean?>,
  scope: CoroutineScope,
) : Tracker {
  override val isRemote: Boolean = false

  private val common = MutableStateFlow<Map<String, String>>(emptyMap())

  private val pending = ArrayDeque<(Tracker) -> Unit>()

  init {
    // Sense aquest collect, la cua només s'enviaria o es buidaria amb la crida següent
    scope.launch { consent.collect { settle(it) } }
  }

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

  private fun dispatch(call: (Tracker) -> Unit) {
    trackers.filter { !it.isRemote }.forEach(call)
    // Primer el que hi havia a la cua, perquè es mantingui l'ordre si el «Sí» arriba abans que el collect
    val toSend =
      synchronized(pending) {
        when (consent.value) {
          null -> {
            if (pending.size == MAX_PENDING) pending.removeFirst()
            pending.addLast(call)
            return
          }
          true -> drain() + call
          false -> {
            pending.clear()
            return
          }
        }
      }
    sendToRemote(toSend)
  }

  private fun settle(consent: Boolean?) {
    val toSend =
      synchronized(pending) {
        when (consent) {
          null -> return
          true -> drain()
          false -> {
            pending.clear()
            return
          }
        }
      }
    sendToRemote(toSend)
  }

  private fun drain(): List<(Tracker) -> Unit> = pending.toList().also { pending.clear() }

  private fun sendToRemote(calls: List<(Tracker) -> Unit>) {
    if (calls.isEmpty()) return
    val remotes = trackers.filter { it.isRemote }
    calls.forEach { call -> remotes.forEach(call) }
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

  companion object {
    const val MAX_PENDING = 100
  }
}

package com.alorma.contactnotes.data.tracking

/**
 * Base de tot el que es pot enviar. Cada esdeveniment concret és una subclasse de [Screen], [Event] o [Action] que
 * construeix les seves [eventProperties] a partir dels seus paràmetres.
 *
 * Mai hi va text que hagi escrit l'usuari, noms de persones, ids ni cap altra dada: només valors tancats
 * (`"quick_note"`, `"said"`...). Una persona protegida compta igual que qualsevol altra.
 *
 * [eventVersion] s'apuja quan canvia el significat o les propietats de l'esdeveniment.
 */
abstract class TrackingEvent(
  val eventName: String,
  val eventVersion: Int,
  val eventProperties: Map<String, String> = emptyMap(),
) {
  override fun toString(): String = "${this::class.simpleName}($eventName v$eventVersion $eventProperties)"
}

/** Una pantalla que s'obre. */
abstract class Screen(
  eventName: String,
  eventVersion: Int,
  eventProperties: Map<String, String> = emptyMap(),
) : TrackingEvent(eventName, eventVersion, eventProperties)

/** Una cosa que passa sense que l'usuari la demani directament (l'app es bloqueja, es restaura el Pro...). */
abstract class Event(
  eventName: String,
  eventVersion: Int,
  eventProperties: Map<String, String> = emptyMap(),
) : TrackingEvent(eventName, eventVersion, eventProperties)

/** Una acció de l'usuari (desar una nota, arxivar una persona...). */
abstract class Action(
  eventName: String,
  eventVersion: Int,
  eventProperties: Map<String, String> = emptyMap(),
) : TrackingEvent(eventName, eventVersion, eventProperties)

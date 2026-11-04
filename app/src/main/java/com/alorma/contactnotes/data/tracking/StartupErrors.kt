package com.alorma.contactnotes.data.tracking

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences

/**
 * Errors que passen abans que hi hagi [Tracker] (per exemple, mentre `ConsentManager` llegeix el DataStore i Koin
 * encara l'està creant). Es guarden fins que [attach] rep el tracker; a partir d'aquí van directes.
 */
class ErrorQueue {
  private val pending = mutableListOf<Pair<Throwable, String>>()
  private var tracker: Tracker? = null

  fun report(
    error: Throwable,
    where: String,
  ) {
    val current =
      synchronized(this) {
        tracker ?: run {
          pending += error to where
          return
        }
      }
    current.trackError(error, where)
  }

  fun attach(tracker: Tracker) {
    val drained =
      synchronized(this) {
        this.tracker = tracker
        pending.toList().also { pending.clear() }
      }
    drained.forEach { (error, where) -> tracker.trackError(error, where) }
  }
}

/** La cua de l'app: s'hi enganxa l'`EventTracker` a `ConversesApp` just després d'arrencar Koin. */
val StartupErrors = ErrorQueue()

/**
 * Si el fitxer del DataStore està corrupte, es buida i l'app arrenca amb els valors per defecte en comptes de petar
 * cada cop. [where] surt d'[ErrorSites].
 */
fun resetOnCorruption(where: String): ReplaceFileCorruptionHandler<Preferences> =
  ReplaceFileCorruptionHandler { error ->
    StartupErrors.report(error, where)
    emptyPreferences()
  }

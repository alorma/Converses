package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.tracking.FirebaseGateway

/** Es comporta com [com.alorma.contactnotes.data.tracking.AndroidFirebaseGateway] sense tocar Firebase. */
class FakeFirebaseGateway : FirebaseGateway {
  var initialized = false
    private set
  var enabled = false
    private set

  /** Si [disable] ha descartat els pendents d'un Firebase inicialitzat. */
  var discarded = false
    private set

  override fun enable() {
    initialized = true
    enabled = true
  }

  override fun disable() {
    enabled = false
    if (initialized) discarded = true
  }
}

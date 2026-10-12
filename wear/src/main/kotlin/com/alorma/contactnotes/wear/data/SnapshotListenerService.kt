package com.alorma.contactnotes.wear.data

import com.alorma.contactnotes.shared.WEAR_SNAPSHOT_PATH
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/** Rep l'instantani del mòbil per la Data Layer i el desa a la còpia local. No registra mai el contingut. */
class SnapshotListenerService : WearableListenerService() {
  override fun onDataChanged(dataEvents: DataEventBuffer) {
    val store = WearSnapshotStore.get(this)
    for (event in dataEvents) {
      if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != WEAR_SNAPSHOT_PATH) continue
      val payload = event.dataItem.data ?: continue
      // És una escriptura petita i el servei ha de continuar viu fins que acabi
      runBlocking { store.save(payload.toString(Charsets.UTF_8)) }
    }
  }
}

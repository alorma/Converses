package com.alorma.contactnotes.wear

import android.app.Application
import com.alorma.contactnotes.shared.WEAR_WATCH_CAPABILITY
import com.google.android.gms.wearable.Wearable

class ConversesWearApp : Application() {
  override fun onCreate() {
    super.onCreate()
    // A més del recurs `android_wear_capabilities`: així el mòbil sap que hi ha l'app encara que Play Services no hagi
    // rellegit el recurs en instal·lar-la. És idempotent i no falla si no hi ha Play Services.
    Wearable.getCapabilityClient(this).addLocalCapability(WEAR_WATCH_CAPABILITY)
  }
}

package com.alorma.contactnotes.wear

import android.app.Application
import com.alorma.contactnotes.shared.WEAR_WATCH_CAPABILITY
import com.alorma.contactnotes.wear.data.DeliveryScheduler
import com.alorma.contactnotes.wear.data.GmsPhoneConnection
import com.alorma.contactnotes.wear.data.WearNotes
import com.alorma.contactnotes.wear.data.WorkManagerDeliveryScheduler
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class ConversesWearApp : Application() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    // A més del recurs `android_wear_capabilities`: així el mòbil sap que hi ha l'app encara que Play Services no hagi
    // rellegit el recurs en instal·lar-la. És idempotent i no falla si no hi ha Play Services.
    Wearable.getCapabilityClient(this).addLocalCapability(WEAR_WATCH_CAPABILITY)
    retryPendingNotesWhenPhoneIsNear()
  }

  // En arrencar, i cada cop que el mòbil torna, es reintenta l'entrega de les notes que queden a la cua
  private fun retryPendingNotesWhenPhoneIsNear() {
    val outbox = WearNotes.get(this)
    val scheduler: DeliveryScheduler = WorkManagerDeliveryScheduler(this)
    scope.launch {
      GmsPhoneConnection(this@ConversesWearApp).connected.filter { it }.collect {
        if (outbox.hasPending()) scheduler.schedule()
      }
    }
  }
}

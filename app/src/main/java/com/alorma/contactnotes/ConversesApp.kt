package com.alorma.contactnotes

import android.app.Application
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.alorma.contactnotes.data.contacts.ContactsReconciler
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.wear.WearSnapshotPublisher
import com.alorma.contactnotes.di.appModules
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ConversesApp : Application() {
  override fun onCreate() {
    super.onCreate()
    configureRevenueCat()
    startKoin {
      androidContext(this@ConversesApp)
      modules(appModules)
    }
    val wearPublisher: WearSnapshotPublisher by inject()
    wearPublisher.start()
    val contactsReconciler: ContactsReconciler by inject()
    contactsReconciler.start()
    val appLock: AppLockManager by inject()
    ProcessLifecycleOwner.get().lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = appLock.onAppStarted()

        override fun onStop(owner: LifecycleOwner) = appLock.onAppStopped()
      },
    )
  }

  /**
   * Sense `appUserID`: RevenueCat fa servir el seu identificador anònim. Sense clau no es configura i ningú no és Pro.
   * Es configura quan el fil principal queda lliure, després del primer frame: a l'arrencada afegia uns 400 ms.
   */
  private fun configureRevenueCat() {
    if (BuildConfig.REVENUECAT_API_KEY.isBlank()) return
    Looper.myQueue().addIdleHandler {
      if (BuildConfig.DEBUG) Purchases.logLevel = LogLevel.DEBUG
      Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build())
      false
    }
  }
}

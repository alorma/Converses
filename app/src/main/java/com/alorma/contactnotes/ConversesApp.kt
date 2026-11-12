package com.alorma.contactnotes

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.alorma.contactnotes.data.pro.ProDataSource
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.sync.SyncFlag
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncScheduler
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.FirebaseController
import com.alorma.contactnotes.data.tracking.ProTrackingProperty
import com.alorma.contactnotes.data.tracking.StartupErrors
import com.alorma.contactnotes.di.appModules
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ConversesApp :
  Application(),
  Configuration.Provider {
  // WorkManager no arrenca sol (vegeu el manifest): només en demanar-lo, i només amb la sincronització encesa i canvis pendents
  override val workManagerConfiguration: Configuration = Configuration.Builder().build()

  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@ConversesApp)
      modules(appModules)
    }
    // Primer de tot: amb el consentiment ja donat, Firebase s'inicialitza aquí i Crashlytics ja recull la resta
    get<FirebaseController>().start()
    // Abans de buidar StartupErrors, perquè els errors de l'arrencada també portin is_pro
    get<ProTrackingProperty>().start()
    StartupErrors.attach(get<EventTracker>())
    val pro: ProDataSource by inject()
    pro.start()
    // Amb el flag apagat no es crea res de la sincronització; encès, només toca Firebase si el dispositiu té sessió
    if (get<SyncFlag>().isEnabled()) {
      get<SyncRepository>().start()
      get<SyncScheduler>().start(ProcessLifecycleOwner.get().lifecycle)
    }
    val appLock: AppLockManager by inject()
    ProcessLifecycleOwner.get().lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = appLock.onAppStarted()

        override fun onStop(owner: LifecycleOwner) = appLock.onAppStopped()
      },
    )
  }
}

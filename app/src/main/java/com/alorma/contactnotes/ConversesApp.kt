package com.alorma.contactnotes

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.alorma.contactnotes.data.pro.ProDataSource
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.ProTrackingProperty
import com.alorma.contactnotes.data.tracking.StartupErrors
import com.alorma.contactnotes.di.appModules
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ConversesApp : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@ConversesApp)
      modules(appModules)
    }
    // Abans de buidar StartupErrors, perquè els errors de l'arrencada també portin is_pro
    get<ProTrackingProperty>().start()
    StartupErrors.attach(get<EventTracker>())
    val pro: ProDataSource by inject()
    pro.start()
    val appLock: AppLockManager by inject()
    ProcessLifecycleOwner.get().lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = appLock.onAppStarted()

        override fun onStop(owner: LifecycleOwner) = appLock.onAppStopped()
      },
    )
  }
}

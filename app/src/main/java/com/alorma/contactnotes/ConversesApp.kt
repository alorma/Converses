package com.alorma.contactnotes

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.alorma.contactnotes.data.contacts.ContactsReconciler
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.wear.WearSnapshotPublisher
import com.alorma.contactnotes.di.appModules
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
}

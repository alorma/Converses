package com.alorma.contactnotes

import android.app.Application
import com.alorma.contactnotes.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ConversesApp : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@ConversesApp)
      modules(appModules)
    }
  }
}

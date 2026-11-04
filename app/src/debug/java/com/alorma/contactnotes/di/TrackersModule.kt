package com.alorma.contactnotes.di

import com.alorma.contactnotes.data.tracking.AndroidFirebaseSink
import com.alorma.contactnotes.data.tracking.FirebaseTracker
import com.alorma.contactnotes.data.tracking.TimberTracker
import com.alorma.contactnotes.data.tracking.Tracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/** Debug: el logcat, que rep sempre, i Firebase a l'app `.dev`, que només rep amb el consentiment. */
val trackersModule =
  module {
    single { TimberTracker() } bind Tracker::class
    single { FirebaseTracker(get(), AndroidFirebaseSink(androidContext())) } bind Tracker::class
  }

package com.alorma.contactnotes.di

import com.alorma.contactnotes.data.tracking.AndroidFirebaseSink
import com.alorma.contactnotes.data.tracking.FirebaseTracker
import com.alorma.contactnotes.data.tracking.Tracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/** Release: Firebase. */
val trackersModule =
  module {
    single { FirebaseTracker(get(), AndroidFirebaseSink(androidContext())) } bind Tracker::class
  }

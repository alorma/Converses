package com.alorma.contactnotes.di

import com.alorma.contactnotes.data.tracking.TimberTracker
import com.alorma.contactnotes.data.tracking.Tracker
import org.koin.dsl.bind
import org.koin.dsl.module

/** Debug: només el logcat. */
val trackersModule =
  module {
    single { TimberTracker() } bind Tracker::class
  }

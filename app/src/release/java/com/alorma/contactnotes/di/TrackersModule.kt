package com.alorma.contactnotes.di

import com.alorma.contactnotes.data.tracking.FirebaseTracker
import com.alorma.contactnotes.data.tracking.Tracker
import org.koin.dsl.bind
import org.koin.dsl.module

/** Release: Firebase (de moment, buit). */
val trackersModule =
  module {
    single { FirebaseTracker() } bind Tracker::class
  }

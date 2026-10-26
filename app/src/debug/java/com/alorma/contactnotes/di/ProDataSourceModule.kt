package com.alorma.contactnotes.di

import com.alorma.contactnotes.data.pro.LocalProDataSource
import com.alorma.contactnotes.data.pro.ProDataSource
import com.alorma.contactnotes.debug.DebugProStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/** Debug: Pro is switched from the debug menu. */
val proDataSourceModule =
  module {
    single { DebugProStore(androidContext()) }
    single {
      val store: DebugProStore = get()
      LocalProDataSource(initial = store.load(), persist = store::save)
    } bind ProDataSource::class
  }

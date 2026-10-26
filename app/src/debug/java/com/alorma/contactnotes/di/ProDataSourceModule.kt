package com.alorma.contactnotes.di

import com.alorma.contactnotes.BuildConfig
import com.alorma.contactnotes.data.pro.LocalProDataSource
import com.alorma.contactnotes.data.pro.NoProDataSource
import com.alorma.contactnotes.data.pro.OverridableProDataSource
import com.alorma.contactnotes.data.pro.ProDataSource
import com.alorma.contactnotes.data.pro.ProStore
import com.alorma.contactnotes.data.pro.RevenueCatGateway
import com.alorma.contactnotes.data.pro.RevenueCatProDataSource
import com.alorma.contactnotes.debug.DebugProStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Debug: Pro comes from RevenueCat's Test Store (`REVENUECAT_TEST_API_KEY`), unless "Override Pro" is on in the debug
 * menu, and then the "Pro user" switch decides.
 */
val proDataSourceModule =
  module {
    single { DebugProStore(androidContext()) }
    single {
      val store: DebugProStore = get()
      LocalProDataSource(initial = store.load(), persist = store::save)
    }
    single {
      val store: DebugProStore = get()
      val remote =
        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
          NoProDataSource
        } else {
          val cache = ProStore(androidContext())
          RevenueCatProDataSource(
            gateway = RevenueCatGateway(androidContext(), BuildConfig.REVENUECAT_API_KEY, debugLogs = true),
            initial = cache.load(),
            persist = cache::save,
          )
        }
      OverridableProDataSource(
        local = get(),
        remote = remote,
        initialOverride = store.loadOverride(),
        persistOverride = store::saveOverride,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
      )
    } bind ProDataSource::class
  }

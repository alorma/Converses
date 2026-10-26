package com.alorma.contactnotes.di

import com.alorma.contactnotes.BuildConfig
import com.alorma.contactnotes.data.pro.NoProDataSource
import com.alorma.contactnotes.data.pro.ProDataSource
import com.alorma.contactnotes.data.pro.ProStore
import com.alorma.contactnotes.data.pro.RevenueCatGateway
import com.alorma.contactnotes.data.pro.RevenueCatProDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Release: Pro surt de RevenueCat; sense clau (`REVENUECAT_GOOGLE_API_KEY`) ningú no és Pro. */
val proDataSourceModule =
  module {
    single<ProDataSource> {
      if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
        NoProDataSource
      } else {
        val store = ProStore(androidContext())
        RevenueCatProDataSource(
          gateway = RevenueCatGateway(androidContext(), BuildConfig.REVENUECAT_API_KEY, debugLogs = false),
          initial = store.load(),
          persist = store::save,
        )
      }
    }
  }

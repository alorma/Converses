package com.alorma.contactnotes.di

import com.alorma.contactnotes.data.pro.NoProDataSource
import com.alorma.contactnotes.data.pro.ProDataSource
import org.koin.dsl.module

/** Release: ningú no és Pro fins que hi hagi RevenueCat (`docs/pro/fases.md`, fase 2). */
val proDataSourceModule =
  module {
    single<ProDataSource> { NoProDataSource }
  }

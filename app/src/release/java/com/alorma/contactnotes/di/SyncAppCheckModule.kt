package com.alorma.contactnotes.di

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.koin.dsl.module

/** Release: App Check amb Play Integrity; només sincronitzen les instal·lacions que Play reconeix. */
val syncAppCheckModule =
  module {
    single<AppCheckProviderFactory> { PlayIntegrityAppCheckProviderFactory.getInstance() }
  }

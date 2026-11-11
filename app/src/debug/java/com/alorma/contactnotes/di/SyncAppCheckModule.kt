package com.alorma.contactnotes.di

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import org.koin.dsl.module

/**
 * Debug: el proveïdor de debug d'App Check, que escriu un token al logcat a cada instal·lació; cal registrar-lo a la
 * consola de Firebase perquè l'emulador pugui sincronitzar. El `.dev` no és a Play i no passaria Play Integrity.
 */
val syncAppCheckModule =
  module {
    single<AppCheckProviderFactory> { DebugAppCheckProviderFactory.getInstance() }
  }

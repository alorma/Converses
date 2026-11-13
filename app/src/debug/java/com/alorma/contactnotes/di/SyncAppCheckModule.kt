package com.alorma.contactnotes.di

import android.content.Context
import com.alorma.contactnotes.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Debug: el proveïdor de debug d'App Check. El `.dev` no és a Play i no passaria Play Integrity.
 *
 * Sense res més, el proveïdor inventa un token a cada instal·lació neta i cal registrar-lo cada vegada a la consola de
 * Firebase. Amb `APP_CHECK_DEBUG_TOKEN` a `local.properties` (un UUID, registrat una sola vegada amb
 * `firebase appcheck:debugtokens:create`), tots els emuladors i totes les reinstal·lacions fan servir aquest.
 */
val syncAppCheckModule =
  module {
    single<AppCheckProviderFactory> { FixedTokenDebugFactory(androidContext(), BuildConfig.APP_CHECK_DEBUG_TOKEN) }
  }

/**
 * Desa [token] on el proveïdor de debug busca el seu (les preferències `com.google.firebase.appcheck.debug.store.<app>`)
 * abans de crear-lo, perquè no n'inventi cap. Amb [token] buit (la CI) no toca res.
 */
private class FixedTokenDebugFactory(
  private val context: Context,
  private val token: String,
) : AppCheckProviderFactory {
  override fun create(firebaseApp: FirebaseApp): AppCheckProvider {
    if (token.isNotBlank()) {
      context
        .getSharedPreferences(PREFS_PREFIX + firebaseApp.persistenceKey, Context.MODE_PRIVATE)
        .edit()
        .putString(SECRET_KEY, token)
        .commit()
    }
    return DebugAppCheckProviderFactory.getInstance().create(firebaseApp)
  }

  private companion object {
    // Els noms que fa servir `StorageHelper` de firebase-appcheck-debug
    const val PREFS_PREFIX = "com.google.firebase.appcheck.debug.store."
    const val SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
  }
}

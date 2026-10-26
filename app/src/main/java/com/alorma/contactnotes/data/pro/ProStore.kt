package com.alorma.contactnotes.data.pro

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.proDataStore by preferencesDataStore(name = "pro")

private val IsProKey = booleanPreferencesKey("is_pro")

/** Últim estat Pro que va dir RevenueCat. Viu a `files/datastore`, que ja està exclòs del backup. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProStore(
  context: Context,
) {
  private val dataStore = context.applicationContext.proDataStore

  // Un sol fil perquè les escriptures no es desordenin
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  /** Lectura síncrona en arrencar, abans que ningú consulti cap flag. */
  fun load(): Boolean = runBlocking { dataStore.data.first()[IsProKey] ?: false }

  fun save(isPro: Boolean) {
    scope.launch { dataStore.edit { it[IsProKey] = isPro } }
  }
}

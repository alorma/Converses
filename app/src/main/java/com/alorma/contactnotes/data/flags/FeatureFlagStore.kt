package com.alorma.contactnotes.data.flags

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

private val Context.featureFlagsDataStore by preferencesDataStore(name = "feature_flags")

/** Desa els overrides dels flags. Viu a `files/datastore`, que ja està exclòs del backup. */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagStore(context: Context) {
  private val dataStore = context.applicationContext.featureFlagsDataStore

  // Un sol fil perquè les escriptures no es desordenin
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  /** Lectura síncrona en arrencar, abans que ningú consulti cap flag. */
  fun load(): Map<String, Boolean> = runBlocking {
    dataStore.data.first().asMap().mapNotNull { (key, value) -> (value as? Boolean)?.let { key.name to it } }.toMap()
  }

  fun save(overrides: Map<String, Boolean>) {
    scope.launch {
      dataStore.edit { prefs ->
        prefs.clear()
        overrides.forEach { (key, value) -> prefs[booleanPreferencesKey(key)] = value }
      }
    }
  }
}

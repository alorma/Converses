package com.alorma.contactnotes.debug

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

private val Context.debugProDataStore by preferencesDataStore(name = "debug_pro")

private val IsProKey = booleanPreferencesKey("is_pro")

/** Saves the debug menu's fake Pro. Lives in `files/datastore`, already excluded from backup. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugProStore(
  context: Context,
) {
  private val dataStore = context.applicationContext.debugProDataStore

  // A single thread so writes keep their order
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  /** Synchronous read at startup, before any flag is checked. */
  fun load(): Boolean = runBlocking { dataStore.data.first()[IsProKey] ?: false }

  fun save(isPro: Boolean) {
    scope.launch { dataStore.edit { it[IsProKey] = isPro } }
  }
}

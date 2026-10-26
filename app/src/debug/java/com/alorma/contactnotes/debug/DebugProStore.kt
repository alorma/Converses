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

private val OverrideKey = booleanPreferencesKey("override")

/** Saves the debug menu's Pro override. Lives in `files/datastore`, already excluded from backup. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugProStore(
  context: Context,
) {
  private val dataStore = context.applicationContext.debugProDataStore

  // A single thread so writes keep their order
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  /** Synchronous read at startup, before any flag is checked. Off by default: Pro comes from RevenueCat. */
  fun loadOverride(): Boolean = runBlocking { dataStore.data.first()[OverrideKey] ?: false }

  fun saveOverride(overridden: Boolean) {
    scope.launch { dataStore.edit { it[OverrideKey] = overridden } }
  }
}

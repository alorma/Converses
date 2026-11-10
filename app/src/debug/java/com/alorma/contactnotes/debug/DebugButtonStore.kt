package com.alorma.contactnotes.debug

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.debugButtonDataStore by preferencesDataStore(
  name = "debug_button",
  corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private val XKey = floatPreferencesKey("x")
private val YKey = floatPreferencesKey("y")

/** Saves where the floating debug button was dropped. Lives in `files/datastore`, already excluded from backup. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugButtonStore(
  context: Context,
) {
  private val dataStore = context.applicationContext.debugButtonDataStore

  // A single thread so writes keep their order
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  /** Top-start position in px, or null if the button was never moved. */
  fun loadPosition(): Offset? =
    runBlocking {
      val prefs = dataStore.data.first()
      val x = prefs[XKey]
      val y = prefs[YKey]
      if (x != null && y != null) Offset(x, y) else null
    }

  fun savePosition(position: Offset) {
    scope.launch {
      dataStore.edit {
        it[XKey] = position.x
        it[YKey] = position.y
      }
    }
  }
}

package com.alorma.contactnotes.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import kotlinx.coroutines.flow.first

/**
 * El cursor i el `deviceId`, al mateix DataStore `sync` que la sessió: en tancar la sessió s'esborren tots dos, i un
 * compte nou torna a baixar des de zero amb un dispositiu nou.
 */
class DataStoreSyncCursorStore(
  context: Context,
) : SyncCursorStore {
  private val dataStore = context.applicationContext.syncDataStore

  override suspend fun cursor(): Long = dataStore.data.first()[CURSOR] ?: 0L

  override suspend fun setCursor(value: Long) {
    dataStore.edit { it[CURSOR] = value }
  }

  override suspend fun deviceId(): String {
    dataStore.data.first()[DEVICE_ID]?.let { return it }
    var created = ""
    dataStore.edit { prefs ->
      created = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().also { prefs[DEVICE_ID] = it }
    }
    return created
  }

  private companion object {
    val CURSOR = longPreferencesKey("cursor")
    val DEVICE_ID = stringPreferencesKey("device_id")
  }
}

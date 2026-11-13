package com.alorma.contactnotes.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * El cursor, al mateix DataStore `sync` que la sessió: en tancar la sessió s'esborra, i un compte nou torna a baixar
 * des de zero. El `deviceId` és de [DeviceStore] i sobreviu.
 */
class DataStoreSyncCursorStore(
  context: Context,
  private val devices: DeviceStore,
) : SyncCursorStore {
  private val dataStore = context.applicationContext.syncDataStore

  override suspend fun cursor(): Long = dataStore.data.first()[CURSOR] ?: 0L

  override suspend fun setCursor(value: Long) {
    dataStore.edit { it[CURSOR] = value }
  }

  override suspend fun deviceId(): String = devices.deviceId()

  private companion object {
    val CURSOR = longPreferencesKey("cursor")
  }
}

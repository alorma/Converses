package com.alorma.contactnotes.data.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.resetOnCorruption
import java.util.UUID
import kotlinx.coroutines.flow.first

/** Només el `deviceId`, a part del DataStore `sync` perquè tancar la sessió no l'esborri. */
internal val Context.syncDeviceDataStore by preferencesDataStore(
  name = "sync_device",
  corruptionHandler = resetOnCorruption(ErrorSites.DATASTORE_SYNC_DEVICE),
)

/**
 * El `deviceId` viu a `sync_device` (sobreviu a tancar la sessió) i `registered` al DataStore `sync` (el `clear()` de
 * [DataStoreSyncLocalStore.setSession] l'esborra). Tots dos són a `files/datastore`, exclòs de la còpia de seguretat.
 *
 * Fins a la fase 4 el `deviceId` era al DataStore `sync`: la primera vegada es copia, perquè els dispositius de prova
 * no perdin la plaça.
 */
class DataStoreDeviceStore(
  context: Context,
) : DeviceStore {
  private val deviceStore = context.applicationContext.syncDeviceDataStore
  private val syncStore = context.applicationContext.syncDataStore

  override suspend fun deviceId(): String {
    deviceStore.data.first()[DEVICE_ID]?.let { return it }
    val legacy = syncStore.data.first()[DEVICE_ID]
    var result = ""
    deviceStore.edit { prefs ->
      result = prefs[DEVICE_ID] ?: (legacy ?: UUID.randomUUID().toString()).also { prefs[DEVICE_ID] = it }
    }
    return result
  }

  override suspend fun isRegistered(): Boolean = syncStore.data.first()[REGISTERED] == true

  override suspend fun setRegistered(value: Boolean) {
    syncStore.edit { it[REGISTERED] = value }
  }

  private companion object {
    val DEVICE_ID = stringPreferencesKey("device_id")
    val REGISTERED = booleanPreferencesKey("registered")
  }
}

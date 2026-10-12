package com.alorma.contactnotes.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.shared.WearSnapshotCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** L'últim instantani rebut i quan es va rebre. */
data class StoredSnapshot(
  val snapshot: WearSnapshot,
  val syncedAt: Long,
)

/**
 * Còpia local del rellotge. Només desa el que ha passat pel filtre de privacitat de [WearSnapshotCodec]: les protegides
 * no hi duen mai contingut. Viu a `files/`, que queda fora de qualsevol còpia de seguretat.
 *
 * Si el Bloqueig de l'app és actiu i el rellotge no té bloqueig de pantalla, no es desa res i s'esborra el que hi
 * hagués ([AppLockGate]), i també la cua de notes pendents ([onLockRequired]): sense bloqueig no en queda cap text.
 */
class WearSnapshotStore(
  private val dataStore: DataStore<Preferences>,
  private val deviceLock: DeviceLock,
  private val onLockRequired: suspend () -> Unit = {},
  private val clock: () -> Long = System::currentTimeMillis,
) {
  val stored: Flow<StoredSnapshot?> =
    dataStore.data.map { prefs ->
      val json = prefs[SnapshotKey] ?: return@map null
      val snapshot = WearSnapshotCodec.decode(json) ?: return@map null
      StoredSnapshot(snapshot, prefs[SyncedAtKey] ?: 0L)
    }

  /**
   * Desa el que arriba del mòbil. Retorna `false` i conserva la còpia anterior si no es pot entendre (text malmès o
   * versió del protocol desconeguda), i `false` **esborrant** la còpia si cal bloqueig i el rellotge no en té.
   */
  suspend fun save(payload: String): Boolean {
    val snapshot = WearSnapshotCodec.decode(payload) ?: return false
    if (isLockRequired(snapshot.appLockActive, deviceLock.isSecure())) {
      eraseEverything()
      return false
    }
    dataStore.edit { prefs ->
      // Es desa el text ja filtrat, no el que ha arribat
      prefs[SnapshotKey] = WearSnapshotCodec.encode(snapshot)
      prefs[SyncedAtKey] = clock()
    }
    return true
  }

  suspend fun clear() {
    dataStore.edit { it.clear() }
  }

  /**
   * Si cal bloqueig i el rellotge no en té, esborra la còpia i la cua de notes pendents i retorna `true`. Sense
   * instantani encara, l'estat no es pot verificar i s'assumeix actiu.
   */
  suspend fun enforceLock(): Boolean {
    val appLockActive = stored.first()?.snapshot?.appLockActive
    val required = isLockRequired(appLockActive, deviceLock.isSecure())
    if (required) eraseEverything()
    return required
  }

  private suspend fun eraseEverything() {
    clear()
    onLockRequired()
  }

  companion object {
    private val SnapshotKey = stringPreferencesKey("snapshot")
    private val SyncedAtKey = longPreferencesKey("synced_at")

    @Volatile
    private var instance: WearSnapshotStore? = null

    fun get(context: Context): WearSnapshotStore =
      instance ?: synchronized(this) {
        instance ?: WearSnapshotStore(
          context.applicationContext.snapshotDataStore,
          AndroidDeviceLock(context),
          onLockRequired = { DataStorePendingNoteQueue.get(context).clear() },
        ).also { instance = it }
      }
  }
}

private val Context.snapshotDataStore by preferencesDataStore(name = "wear_snapshot")

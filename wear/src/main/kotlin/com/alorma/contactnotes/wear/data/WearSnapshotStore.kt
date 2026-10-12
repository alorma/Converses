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
 *
 * En cada sincronització, les notes encuades de persones que ja són protegides o que ja no hi són es descarten
 * ([onPeopleSynced]): una protegida no pot tenir text al disc del rellotge (R3, R4).
 *
 * L'instantani es desa **xifrat** ([cipher], Keystore): amb el rellotge bloquejat és il·legible. Si no es pot xifrar,
 * no es desa res i es conserva la còpia anterior; si no es pot desxifrar, la còpia es tracta com si no hi fos.
 */
class WearSnapshotStore(
  private val dataStore: DataStore<Preferences>,
  private val deviceLock: DeviceLock,
  private val cipher: SnapshotCipher,
  private val onLockRequired: suspend () -> Unit = {},
  private val onPeopleSynced: suspend (Set<Long>) -> Unit = {},
  private val clock: () -> Long = System::currentTimeMillis,
) {
  val stored: Flow<StoredSnapshot?> =
    dataStore.data.map { prefs ->
      val encrypted = prefs[SnapshotKey] ?: return@map null
      val json = cipher.decrypt(encrypted) ?: return@map null
      val snapshot = WearSnapshotCodec.decode(json) ?: return@map null
      StoredSnapshot(snapshot, prefs[SyncedAtKey] ?: 0L)
    }

  /**
   * Desa el que arriba del mòbil. Retorna `false` i conserva la còpia anterior si no es pot entendre (text malmès o
   * versió del protocol desconeguda) o no es pot xifrar (rellotge bloquejat), i `false` **esborrant** la còpia si cal
   * bloqueig i el rellotge no en té.
   */
  suspend fun save(payload: String): Boolean {
    val snapshot = WearSnapshotCodec.decode(payload) ?: return false
    if (isLockRequired(snapshot.appLockActive, deviceLock.isSecure())) {
      eraseEverything()
      return false
    }
    // Es desa el text ja filtrat, no el que ha arribat
    val encrypted = cipher.encrypt(WearSnapshotCodec.encode(snapshot)) ?: return false
    dataStore.edit { prefs ->
      prefs[SnapshotKey] = encrypted
      prefs[SyncedAtKey] = clock()
      // Clau de l'època sense xifratge, que quedava en clar
      prefs.remove(LegacySnapshotKey)
    }
    onPeopleSynced(
      snapshot.people
        .filterNot { it.isProtected }
        .map { it.id }
        .toSet(),
    )
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

  /** Esborra la còpia en clar de quan l'instantani encara no es xifrava. Es crida en arrencar l'app. */
  suspend fun removeLegacyCopy() {
    dataStore.edit { it.remove(LegacySnapshotKey) }
  }

  private suspend fun eraseEverything() {
    clear()
    onLockRequired()
  }

  companion object {
    private val SnapshotKey = stringPreferencesKey("snapshot_encrypted")
    private val LegacySnapshotKey = stringPreferencesKey("snapshot")
    private val SyncedAtKey = longPreferencesKey("synced_at")

    @Volatile
    private var instance: WearSnapshotStore? = null

    fun get(context: Context): WearSnapshotStore =
      instance ?: synchronized(this) {
        instance ?: WearSnapshotStore(
          context.applicationContext.snapshotDataStore,
          AndroidDeviceLock(context),
          KeystoreSnapshotCipher(),
          onLockRequired = { DataStorePendingNoteQueue.get(context).clear() },
          onPeopleSynced = { DataStorePendingNoteQueue.get(context).retainOnly(it) },
        ).also { instance = it }
      }
  }
}

private val Context.snapshotDataStore by preferencesDataStore(name = "wear_snapshot")

package com.alorma.contactnotes.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Quantes notes de la cua s'han descartat sense entregar-se (persona protegida o esborrada) i l'usuari encara no ho ha vist. */
interface DiscardedNotes {
  val count: Flow<Int>

  suspend fun add(amount: Int)

  suspend fun clear()
}

/** Només es desa un número: res del contingut ni de la persona. Viu al mateix DataStore que la cua, fora de la còpia de seguretat. */
class DataStoreDiscardedNotes(
  private val dataStore: DataStore<Preferences>,
) : DiscardedNotes {
  override val count: Flow<Int> = dataStore.data.map { it[CountKey] ?: 0 }

  override suspend fun add(amount: Int) {
    if (amount <= 0) return
    dataStore.edit { it[CountKey] = (it[CountKey] ?: 0) + amount }
  }

  override suspend fun clear() {
    dataStore.edit { it.remove(CountKey) }
  }

  companion object {
    private val CountKey = intPreferencesKey("discarded")

    @Volatile
    private var instance: DataStoreDiscardedNotes? = null

    fun get(context: Context): DataStoreDiscardedNotes =
      instance ?: synchronized(this) {
        instance ?: DataStoreDiscardedNotes(context.applicationContext.pendingNotesDataStore).also { instance = it }
      }
  }
}

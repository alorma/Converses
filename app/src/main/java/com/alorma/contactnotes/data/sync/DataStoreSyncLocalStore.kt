package com.alorma.contactnotes.data.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.resetOnCorruption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore(
  name = "sync",
  corruptionHandler = resetOnCorruption(ErrorSites.DATASTORE_SYNC),
)

/** Viu a `files/datastore`, que ja està exclòs de la còpia de seguretat. Si es corromp, el dispositiu queda sense sessió. */
class DataStoreSyncLocalStore(
  context: Context,
) : SyncLocalStore {
  private val dataStore = context.applicationContext.syncDataStore

  override val wordsReviewedAt: Flow<Long?> = dataStore.data.map { it[WORDS_REVIEWED_AT] }

  override suspend fun hasSession(): Boolean = dataStore.data.first()[SESSION] == true

  override suspend fun setSession(active: Boolean) {
    dataStore.edit { prefs ->
      if (active) {
        prefs[SESSION] = true
      } else {
        prefs.clear()
      }
    }
  }

  override suspend fun setWordsReviewedAt(millis: Long) {
    dataStore.edit { it[WORDS_REVIEWED_AT] = millis }
  }

  private companion object {
    val SESSION = booleanPreferencesKey("session")
    val WORDS_REVIEWED_AT = longPreferencesKey("words_reviewed_at")
  }
}

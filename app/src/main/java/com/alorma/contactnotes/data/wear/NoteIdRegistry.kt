package com.alorma.contactnotes.data.wear

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** UUID de les notes del rellotge ja desades, per ignorar els reenviaments. Només guarda ids, mai contingut. */
interface NoteIdRegistry {
  suspend fun contains(id: String): Boolean

  suspend fun add(id: String)
}

private val Context.wearNotesDataStore by preferencesDataStore(name = "wear_notes")

private val SeenIdsKey = stringPreferencesKey("seen_ids")

/** Últims [MaxIds] ids, en un DataStore a `files/datastore` (ja exclòs del backup). Sense canvis a Room. */
class DataStoreNoteIdRegistry(
  context: Context,
) : NoteIdRegistry {
  private val dataStore = context.applicationContext.wearNotesDataStore

  override suspend fun contains(id: String): Boolean = id in read(dataStore.data.first()[SeenIdsKey])

  override suspend fun add(id: String) {
    dataStore.edit { prefs ->
      val ids = read(prefs[SeenIdsKey])
      if (id !in ids) prefs[SeenIdsKey] = (ids + id).takeLast(MaxIds).joinToString(SEPARATOR)
    }
  }

  private fun read(raw: String?): List<String> = raw?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()

  private companion object {
    const val MaxIds = 500
    const val SEPARATOR = "\n"
  }
}

package com.alorma.contactnotes.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.contactnotes.shared.WearNote
import com.alorma.contactnotes.shared.WearNoteCodec
import kotlinx.coroutines.flow.first

/**
 * Notes dictades que encara no s'han entregat al mòbil (P14). Persisteixen fins que el mòbil les confirma.
 * És una interfície perquè la lògica d'entrega es pugui provar.
 */
interface PendingNoteQueue {
  /** De la més antiga a la més nova. */
  suspend fun all(): List<WearNote>

  suspend fun add(note: WearNote)

  suspend fun remove(id: String)

  /** Esborra tota la cua: passa quan cal bloqueig al rellotge i no en té. Les notes es perden. */
  suspend fun clear()
}

/**
 * Cua a `files/datastore`, fora de qualsevol còpia de seguretat. Cada nota es desa com a JSON, una per línia.
 * Si el Bloqueig de l'app és actiu i el rellotge no té bloqueig, s'esborra sencera ([WearSnapshotStore.enforceLock]).
 */
class DataStorePendingNoteQueue(
  private val dataStore: DataStore<Preferences>,
) : PendingNoteQueue {
  override suspend fun all(): List<WearNote> = read(dataStore.data.first()[QueueKey])

  override suspend fun add(note: WearNote) {
    dataStore.edit { prefs ->
      val notes = read(prefs[QueueKey])
      if (notes.none { it.id == note.id }) prefs[QueueKey] = write(notes + note)
    }
  }

  override suspend fun remove(id: String) {
    dataStore.edit { prefs ->
      val notes = read(prefs[QueueKey])
      if (notes.any { it.id == id }) prefs[QueueKey] = write(notes.filterNot { it.id == id })
    }
  }

  override suspend fun clear() {
    dataStore.edit { it.remove(QueueKey) }
  }

  // Les notes mal formades es descarten: no es pot fer res amb elles i bloquejarien la cua
  private fun read(raw: String?): List<WearNote> =
    raw
      ?.lineSequence()
      ?.filter { it.isNotBlank() }
      ?.mapNotNull(WearNoteCodec::decodeNote)
      ?.toList()
      .orEmpty()

  // El JSON de kotlinx escapa els salts de línia del text, així que una nota ocupa sempre una línia
  private fun write(notes: List<WearNote>): String = notes.joinToString("\n", transform = WearNoteCodec::encodeNote)

  companion object {
    private val QueueKey = stringPreferencesKey("queue")

    @Volatile
    private var instance: DataStorePendingNoteQueue? = null

    fun get(context: Context): DataStorePendingNoteQueue =
      instance ?: synchronized(this) {
        instance ?: DataStorePendingNoteQueue(context.applicationContext.pendingNotesDataStore).also { instance = it }
      }
  }
}

private val Context.pendingNotesDataStore by preferencesDataStore(name = "wear_pending_notes")

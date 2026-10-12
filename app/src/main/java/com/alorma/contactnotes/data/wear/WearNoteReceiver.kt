package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.WearNoteCodec
import com.alorma.contactnotes.shared.WearNoteResponse
import com.alorma.contactnotes.shared.WearNoteResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Desa les notes que arriben del rellotge. Només a persones existents, no arxivades i no protegides (una protegida
 * no rep res del rellotge fins que la fase 5 en tingui l'aprovació). Idempotent per UUID. No registra contingut.
 * El canal és el de la nota ràpida: l'últim usat amb la persona, o el primer.
 */
class WearNoteReceiver(
  private val repository: ConversesRepository,
  private val registry: NoteIdRegistry,
) {
  // Dos reenviaments alhora no poden desar la nota dues vegades
  private val lock = Mutex()

  /** `null` si el missatge és il·legible: no es respon res i el rellotge ho reintenta. */
  suspend fun receive(payload: String): String? {
    val note = WearNoteCodec.decodeNote(payload) ?: return null
    val result =
      lock.withLock {
        val person = repository.getPerson(note.personId)
        when {
          registry.contains(note.id) -> WearNoteResult.SAVED
          person == null || person.isProtected || person.archived -> WearNoteResult.REJECTED
          else -> {
            val channel =
              repository
                .observeTimeline(person.id)
                .first()
                .firstOrNull()
                ?.channelType ?: ChannelType.entries.first()
            repository.addEntry(
              EntryEntity(
                personId = person.id,
                kind = note.who,
                channelType = channel,
                text = note.text.trim(),
                occurredAt = note.timestamp,
              ),
            )
            registry.add(note.id)
            WearNoteResult.SAVED
          }
        }
      }
    return WearNoteCodec.encodeResponse(WearNoteResponse(result = result))
  }
}

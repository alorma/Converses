package com.alorma.contactnotes.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Camí del missatge (`MessageClient`) amb el qual el rellotge envia una nota dictada al mòbil. */
const val WEAR_NOTE_PATH = "/converses/note"

/** Mida màxima d'una nota del rellotge. No es talla en silenci com les entrades del rellotge: si passa, es rebutja. */
const val WEAR_MAX_NOTE_TEXT_LENGTH = 4_000

/**
 * Nota dictada al rellotge. [id] és un UUID únic per nota: el mòbil l'usa per ignorar duplicats.
 * [who] és el tipus d'entrada triat a «Qui»; el canal el decideix el mòbil.
 */
@Serializable
data class WearNote(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val id: String,
  val personId: Long,
  val who: EntryKind,
  val text: String,
  val timestamp: Long,
)

enum class WearNoteResult {
  /** Desada, o ja desada abans (mateix UUID). El rellotge l'esborra de la cua. */
  SAVED,

  /** La persona no existeix, és arxivada o és protegida, o la nota no és vàlida. No té sentit reintentar. */
  REJECTED,
}

@Serializable
data class WearNoteResponse(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val result: WearNoteResult,
)

/** Una nota és vàlida si té UUID, persona i un text no buit que no passa del límit. */
fun WearNote.isValid(): Boolean =
  id.isNotBlank() && id.length <= 64 && text.isNotBlank() && text.length <= WEAR_MAX_NOTE_TEXT_LENGTH

/** Serialització de la nota i la resposta; tolerant amb camps nous, com [WearEntriesCodec]. */
object WearNoteCodec {
  private val json =
    Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

  fun encodeNote(note: WearNote): String = json.encodeToString(WearNote.serializer(), note)

  /** `null` si el text està malmès, la versió no és la d'aquesta app o la nota no és vàlida. */
  fun decodeNote(text: String): WearNote? =
    try {
      json.decodeFromString(WearNote.serializer(), text).takeIf { it.version == WEAR_PROTOCOL_VERSION && it.isValid() }
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }

  fun encodeResponse(response: WearNoteResponse): String = json.encodeToString(WearNoteResponse.serializer(), response)

  fun decodeResponse(text: String): WearNoteResponse? =
    try {
      json.decodeFromString(WearNoteResponse.serializer(), text).takeIf { it.version == WEAR_PROTOCOL_VERSION }
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
}

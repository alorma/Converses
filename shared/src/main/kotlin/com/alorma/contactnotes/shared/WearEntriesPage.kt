package com.alorma.contactnotes.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Camí del missatge (`MessageClient`) amb el qual el rellotge demana més entrades d'una persona. */
const val WEAR_ENTRIES_PATH = "/converses/entries"

/** Entrades per pàgina de «més entrades». */
const val WEAR_ENTRIES_PAGE_SIZE = 20

/** Màxim d'entrades extra que el rellotge manté en memòria per persona (a més de les de la còpia). */
const val WEAR_MAX_EXTRA_ENTRIES = 100

/**
 * Petició d'una pàgina d'entrades anteriors a ([beforeOccurredAt], [beforeId]), de la més nova a la més antiga.
 * El cursor és el de l'última entrada que el rellotge ja té.
 */
@Serializable
data class WearEntriesRequest(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val personId: Long,
  val beforeOccurredAt: Long,
  val beforeId: Long,
  val limit: Int = WEAR_ENTRIES_PAGE_SIZE,
)

/**
 * Resposta del mòbil. [denied] vol dir que no serveix res d'aquesta persona (protegida, arxivada o inexistent):
 * en aquest cas [entries] és sempre buit.
 */
@Serializable
data class WearEntriesResponse(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val entries: List<WearEntry> = emptyList(),
  val hasMore: Boolean = false,
  val denied: Boolean = false,
)

/** Aplica els límits de privacitat i de mida a qualsevol pàgina, vingui d'on vingui. */
fun WearEntriesResponse.sanitized(): WearEntriesResponse =
  if (denied) {
    WearEntriesResponse(version = version, denied = true)
  } else {
    copy(
      entries =
        entries
          .sortedWith(compareByDescending<WearEntry> { it.occurredAt }.thenByDescending { it.id })
          .take(WEAR_ENTRIES_PAGE_SIZE)
          .map { it.copy(text = it.text.truncatedForWear()) },
    )
  }

/** Serialització de les peticions i respostes de «més entrades»; tolerant amb camps nous, com [WearSnapshotCodec]. */
object WearEntriesCodec {
  private val json =
    Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

  fun encodeRequest(request: WearEntriesRequest): String = json.encodeToString(WearEntriesRequest.serializer(), request)

  /** `null` si el text està malmès o la versió no és la d'aquesta app. El límit es redueix a la mida de pàgina. */
  fun decodeRequest(text: String): WearEntriesRequest? =
    decode(text, WearEntriesRequest.serializer(), WearEntriesRequest::version)
      ?.let { it.copy(limit = it.limit.coerceIn(1, WEAR_ENTRIES_PAGE_SIZE)) }

  fun encodeResponse(response: WearEntriesResponse): String =
    json.encodeToString(WearEntriesResponse.serializer(), response.sanitized())

  fun decodeResponse(text: String): WearEntriesResponse? =
    decode(text, WearEntriesResponse.serializer(), WearEntriesResponse::version)?.sanitized()

  private fun <T> decode(
    text: String,
    serializer: kotlinx.serialization.KSerializer<T>,
    version: (T) -> Int,
  ): T? =
    try {
      json.decodeFromString(serializer, text).takeIf { version(it) == WEAR_PROTOCOL_VERSION }
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
}

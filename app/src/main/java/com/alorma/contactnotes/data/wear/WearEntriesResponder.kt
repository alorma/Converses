package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.shared.WearEntriesCodec
import com.alorma.contactnotes.shared.WearEntriesResponse
import com.alorma.contactnotes.shared.WearEntry

/**
 * Respon les peticions de «més entrades» del rellotge. Aquí és el filtre de privacitat d'aquest canal: una persona
 * protegida, arxivada o inexistent mai serveix res (`denied`), i no es registra mai contingut.
 */
class WearEntriesResponder(
  private val repository: ConversesRepository,
) {
  /** `null` si la petició és il·legible: no es respon res. */
  suspend fun respond(payload: String): String? {
    val request = WearEntriesCodec.decodeRequest(payload) ?: return null
    val person = repository.getPerson(request.personId)
    if (person == null || person.isProtected || person.archived) {
      return WearEntriesCodec.encodeResponse(WearEntriesResponse(denied = true))
    }
    // Una entrada de més per saber si en queden
    val page =
      repository.getEntriesBefore(request.personId, request.beforeOccurredAt, request.beforeId, request.limit + 1)
    return WearEntriesCodec.encodeResponse(
      WearEntriesResponse(
        entries = page.take(request.limit).map { WearEntry(it.id, it.kind, it.channelType, it.text, it.occurredAt) },
        hasMore = page.size > request.limit,
      ),
    )
  }
}

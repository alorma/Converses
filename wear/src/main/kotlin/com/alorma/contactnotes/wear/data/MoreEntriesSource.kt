package com.alorma.contactnotes.wear.data

import android.content.Context
import com.alorma.contactnotes.shared.WEAR_ENTRIES_PATH
import com.alorma.contactnotes.shared.WearEntriesCodec
import com.alorma.contactnotes.shared.WearEntriesRequest
import com.alorma.contactnotes.shared.WearEntriesResponse
import com.alorma.contactnotes.shared.WearEntry
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Resultat de demanar més entrades al mòbil. */
sealed interface MoreEntriesResult {
  /** Una pàgina, de la més nova a la més antiga; [hasMore] diu si en queden més al mòbil. */
  data class Page(
    val entries: List<WearEntry>,
    val hasMore: Boolean,
  ) : MoreEntriesResult

  /** No hi ha mòbil, no ha respost a temps o no serveix res d'aquesta persona: la llista s'acaba sense error (P13). */
  data object Unavailable : MoreEntriesResult
}

/** Demana al mòbil les entrades anteriors a un cursor. És una interfície perquè els ViewModels es puguin provar. */
interface MoreEntriesSource {
  suspend fun load(
    personId: Long,
    beforeOccurredAt: Long,
    beforeId: Long,
  ): MoreEntriesResult
}

/** Una resposta il·legible o denegada és el mateix que no tenir mòbil. */
fun WearEntriesResponse?.toResult(): MoreEntriesResult =
  if (this == null || denied) MoreEntriesResult.Unavailable else MoreEntriesResult.Page(entries, hasMore)

class GmsMoreEntriesSource(
  context: Context,
  private val timeoutMillis: Long = TIMEOUT_MILLIS,
) : MoreEntriesSource {
  private val appContext = context.applicationContext

  override suspend fun load(
    personId: Long,
    beforeOccurredAt: Long,
    beforeId: Long,
  ): MoreEntriesResult =
    try {
      val nodeId = nearbyPhoneNodeId(appContext) ?: return MoreEntriesResult.Unavailable
      val request =
        WearEntriesCodec
          .encodeRequest(
            WearEntriesRequest(personId = personId, beforeOccurredAt = beforeOccurredAt, beforeId = beforeId),
          ).toByteArray(Charsets.UTF_8)
      val reply =
        withTimeout(timeoutMillis) {
          Wearable.getMessageClient(appContext).sendRequest(nodeId, WEAR_ENTRIES_PATH, request).await()
        }
      WearEntriesCodec.decodeResponse(reply.toString(Charsets.UTF_8)).toResult()
    } catch (_: ApiException) {
      MoreEntriesResult.Unavailable
    } catch (_: TimeoutCancellationException) {
      MoreEntriesResult.Unavailable
    }

  private companion object {
    const val TIMEOUT_MILLIS = 8_000L
  }
}

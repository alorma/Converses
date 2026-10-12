package com.alorma.contactnotes.wear.data

import android.content.Context
import com.alorma.contactnotes.shared.WEAR_NOTE_PATH
import com.alorma.contactnotes.shared.WearNote
import com.alorma.contactnotes.shared.WearNoteCodec
import com.alorma.contactnotes.shared.WearNoteResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Què ha passat en enviar una nota al mòbil. */
enum class NoteDelivery {
  /** El mòbil l'ha desada (o ja la tenia). */
  Delivered,

  /** El mòbil no la vol (persona inexistent, arxivada o protegida): reintentar no serveix de res. */
  Rejected,

  /** No hi ha mòbil o no ha respost: es reintentarà. */
  Unavailable,
}

/** Envia una nota al mòbil. És una interfície perquè la cua es pugui provar sense Play Services. */
fun interface NoteSender {
  suspend fun send(note: WearNote): NoteDelivery
}

class GmsNoteSender(
  context: Context,
  private val timeoutMillis: Long = TIMEOUT_MILLIS,
) : NoteSender {
  private val appContext = context.applicationContext

  override suspend fun send(note: WearNote): NoteDelivery =
    try {
      val nodeId = nearbyPhoneNodeId(appContext) ?: return NoteDelivery.Unavailable
      val reply =
        withTimeout(timeoutMillis) {
          Wearable
            .getMessageClient(appContext)
            .sendRequest(nodeId, WEAR_NOTE_PATH, WearNoteCodec.encodeNote(note).toByteArray(Charsets.UTF_8))
            .await()
        }
      when (WearNoteCodec.decodeResponse(reply.toString(Charsets.UTF_8))?.result) {
        WearNoteResult.SAVED -> NoteDelivery.Delivered
        WearNoteResult.REJECTED -> NoteDelivery.Rejected
        null -> NoteDelivery.Unavailable
      }
    } catch (_: ApiException) {
      NoteDelivery.Unavailable
    } catch (_: TimeoutCancellationException) {
      NoteDelivery.Unavailable
    }

  private companion object {
    const val TIMEOUT_MILLIS = 8_000L
  }
}

/** El que necessita la pantalla «Qui»: deixar una nota a la cua sense preocupar-se de l'entrega. */
fun interface NoteSubmitter {
  suspend fun submit(note: WearNote)
}

/** Demana que s'intenti l'entrega de la cua. La implementació real és un `WorkManager` (sobreviu al tancament de l'app). */
fun interface DeliveryScheduler {
  fun schedule()
}

/**
 * Cua d'entrega de notes: es desa primer a disc i després s'intenta l'enviament. Una nota només surt de la cua quan el
 * mòbil la confirma o la rebutja. Els reenviaments són segurs: el mòbil ignora els UUID que ja té.
 */
class NoteOutbox(
  private val queue: PendingNoteQueue,
  private val sender: NoteSender,
  private val discarded: DiscardedNotes? = null,
  private val scheduler: DeliveryScheduler,
) : NoteSubmitter {
  /** Desa la nota a la cua i en programa l'entrega. Quan torna, la nota ja no es perd encara que l'app es tanqui. */
  override suspend fun submit(note: WearNote) {
    queue.add(note)
    scheduler.schedule()
  }

  /** Cal programar un nou intent (en arrencar, o en tornar el mòbil) si queda res a la cua. */
  suspend fun hasPending(): Boolean = queue.all().isNotEmpty()

  /**
   * Intenta entregar tota la cua, de la més antiga a la més nova. Retorna `true` si ha quedat buida; `false` si cal
   * reintentar (el mòbil no hi és): en aquest cas no es prova la resta, perquè fallaria igual.
   */
  suspend fun deliverAll(): Boolean {
    for (note in queue.all()) {
      when (sender.send(note)) {
        NoteDelivery.Delivered -> queue.remove(note.id)
        NoteDelivery.Rejected -> {
          queue.remove(note.id)
          discarded?.add(1)
        }
        NoteDelivery.Unavailable -> return false
      }
    }
    return queue.all().isEmpty()
  }
}

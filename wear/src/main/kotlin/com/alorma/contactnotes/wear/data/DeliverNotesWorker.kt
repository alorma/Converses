package com.alorma.contactnotes.wear.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Entrega la cua de notes. Reintenta amb espera creixent mentre el mòbil no respongui. */
class DeliverNotesWorker(
  context: Context,
  params: WorkerParameters,
) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result =
    if (WearNotes.get(applicationContext).deliverAll()) Result.success() else Result.retry()
}

/** [DeliveryScheduler] sobre WorkManager: una sola feina única; si n'hi ha una en marxa, es substitueix (l'enviament és idempotent). */
class WorkManagerDeliveryScheduler(
  context: Context,
) : DeliveryScheduler {
  private val workManager = WorkManager.getInstance(context.applicationContext)

  override fun schedule() {
    val request =
      OneTimeWorkRequestBuilder<DeliverNotesWorker>()
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
        .build()
    workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
  }

  private companion object {
    const val UNIQUE_NAME = "deliver-notes"
    const val BACKOFF_SECONDS = 30L
  }
}

/** Accés únic a la cua d'entrega per a l'app i el worker. */
object WearNotes {
  @Volatile
  private var instance: NoteOutbox? = null

  fun get(context: Context): NoteOutbox =
    instance ?: synchronized(this) {
      instance ?: NoteOutbox(
        queue = DataStorePendingNoteQueue.get(context),
        sender = GmsNoteSender(context),
        scheduler = WorkManagerDeliveryScheduler(context),
      ).also { instance = it }
    }
}

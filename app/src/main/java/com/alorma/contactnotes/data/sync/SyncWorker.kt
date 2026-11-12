package com.alorma.contactnotes.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * La sincronització de fons, per a quan l'app es tanca amb canvis pendents. Sense flag o sense sessió no fa res. Agafa
 * les dependències de Koin: el procés pot haver nascut només per això, i `ConversesApp` ja ha recuperat la sessió.
 */
class SyncWorker(
  context: Context,
  params: WorkerParameters,
) : CoroutineWorker(context, params),
  KoinComponent {
  private val flag: SyncFlag by inject()
  private val repository: SyncRepository by inject()

  override suspend fun doWork(): Result {
    if (!flag.isEnabled()) return Result.success()
    // Abans de saber si hi ha sessió, l'estat és Checking
    if (repository.state.first { it !is SyncState.Checking } !is SyncState.Ready) return Result.success()
    return when (repository.syncNow()) {
      SyncOutcome.Done -> Result.success()
      SyncOutcome.Offline, SyncOutcome.Failed ->
        if (runAttemptCount <
          MAX_ATTEMPTS
        ) {
          Result.retry()
        } else {
          Result.failure()
        }
    }
  }

  companion object {
    private const val NAME = "sync"
    private const val MAX_ATTEMPTS = 5

    /** Una sola feina alhora; si ja n'hi ha una a la cua es queda. És l'únic lloc que inicialitza WorkManager. */
    fun enqueue(context: Context) {
      val request =
        OneTimeWorkRequestBuilder<SyncWorker>()
          .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
          .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
          .build()
      WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
    }
  }
}

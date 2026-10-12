package com.alorma.contactnotes.data.wear

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alorma.contactnotes.R
import com.alorma.contactnotes.shared.WEAR_APPROVAL_TTL_MS
import com.alorma.contactnotes.ui.approval.ApprovalActivity

/**
 * Notificació d'aprovació: **genèrica, sense nom** ni cap contingut, perquè no filtri res a la pantalla de bloqueig.
 * En tocar-la s'obre [ApprovalActivity] amb l'identificador de la petició; caduca sola amb la petició.
 */
class AndroidApprovalNotifier(
  context: Context,
) : ApprovalNotifier {
  private val appContext = context.applicationContext

  @SuppressLint("MissingPermission")
  override fun show(requestId: String) {
    val manager = NotificationManagerCompat.from(appContext)
    // Sense permís de notificacions (Android 13+) o amb elles apagades, no es pot avisar: la petició caducarà
    if (!manager.areNotificationsEnabled()) return
    createChannel(manager)
    val open =
      PendingIntent.getActivity(
        appContext,
        requestId.hashCode(),
        ApprovalActivity.intent(appContext, requestId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    val title = appContext.getString(R.string.approval_notification_title)
    val notification =
      NotificationCompat
        .Builder(appContext, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_approval)
        .setContentTitle(title)
        .setContentIntent(open)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setTimeoutAfter(WEAR_APPROVAL_TTL_MS)
        .build()
    manager.notify(requestId.hashCode(), notification)
  }

  override fun cancel(requestId: String) {
    NotificationManagerCompat.from(appContext).cancel(requestId.hashCode())
  }

  private fun createChannel(manager: NotificationManagerCompat) {
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        appContext.getString(R.string.approval_channel_name),
        NotificationManager.IMPORTANCE_HIGH,
      ),
    )
  }

  private companion object {
    const val CHANNEL_ID = "wear_approval"
  }
}

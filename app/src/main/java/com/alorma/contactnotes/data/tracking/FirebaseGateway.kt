package com.alorma.contactnotes.data.tracking

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * L'única porta a l'arrencada i l'aturada de Firebase, perquè [FirebaseController] i [FirebaseTracker] es puguin
 * provar a la JVM. Firebase no s'inicialitza sol (el `FirebaseInitProvider` és fora del manifest): només ho fa
 * [enable], i només s'hi arriba amb el consentiment.
 */
interface FirebaseGateway {
  /** Inicialitza Firebase si encara no ho estava i n'activa la recollida. Es pot cridar tantes vegades com calgui. */
  fun enable()

  /**
   * Si Firebase està inicialitzat, n'apaga la recollida i descarta el que tenia pendent d'enviar. Si no ho està, no
   * fa res: no s'inicialitza mai per apagar-lo.
   */
  fun disable()
}

class AndroidFirebaseGateway(
  private val context: Context,
) : FirebaseGateway {
  private var enabled = false

  @Synchronized
  override fun enable() {
    if (enabled) return
    if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
    FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
    enabled = true
  }

  @Synchronized
  override fun disable() {
    enabled = false
    if (FirebaseApp.getApps(context).isEmpty()) return
    FirebaseAnalytics.getInstance(context).run {
      setAnalyticsCollectionEnabled(false)
      resetAnalyticsData()
    }
    FirebaseCrashlytics.getInstance().run {
      isCrashlyticsCollectionEnabled = false
      deleteUnsentReports()
    }
  }
}

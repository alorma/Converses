package com.alorma.contactnotes.data.tracking

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.CustomKeysAndValues
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
    if (!hasDefaultApp()) {
      FirebaseApp.initializeApp(context)
      sanitizeFatalCrashes()
    }
    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
    FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
    enabled = true
  }

  /**
   * Crashlytics instal·la el seu handler en inicialitzar-se (el `getInstance` ho garanteix); el nostre va al damunt,
   * un sol cop per procés, perquè els crashes fatals també surtin sense `message`.
   */
  private fun sanitizeFatalCrashes() {
    FirebaseCrashlytics.getInstance()
    val crashlytics = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler(
      SanitizingExceptionHandler(crashlytics) { Log.e(TAG, "Uncaught exception", it) },
    )
  }

  /**
   * Només la instància per defecte, que és on viuen Analytics i Crashlytics. La sincronització té la seva, amb nom
   * (`SyncFirebase`), i que existeixi no vol dir que l'usuari hagi dit que sí a les estadístiques.
   */
  private fun hasDefaultApp(): Boolean = FirebaseApp.getApps(context).any { it.name == FirebaseApp.DEFAULT_APP_NAME }

  private companion object {
    const val TAG = "Converses"
  }

  @Synchronized
  override fun disable() {
    enabled = false
    if (!hasDefaultApp()) return
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

class AndroidFirebaseSink(
  private val context: Context,
) : FirebaseSink {
  override fun logEvent(
    name: String,
    params: Map<String, String>,
  ) {
    val bundle = Bundle().apply { params.forEach { (key, value) -> putString(key, value) } }
    FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
  }

  override fun recordError(
    error: Throwable,
    keys: Map<String, String>,
  ) {
    val customKeys = CustomKeysAndValues.Builder().apply { keys.forEach { (key, value) -> putString(key, value) } }
    FirebaseCrashlytics.getInstance().recordException(error, customKeys.build())
  }
}

package com.alorma.contactnotes.data.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * La instància `sync` de Firebase, on viuen Auth, Firestore i App Check. És a part de la per defecte (Analytics i
 * Crashlytics, que només arrenca amb el «Sí» a les estadístiques) i aquí no es toca mai: iniciar sessió per
 * sincronitzar no engega cap estadística.
 *
 * No es crea en construir la classe sinó la primera vegada que algú demana [app], [auth] o [firestore]; per això qui
 * no sincronitza no la inicialitza mai.
 */
class SyncFirebase(
  private val context: Context,
  private val appCheckProvider: () -> AppCheckProviderFactory,
) {
  /** Si falla (un `google-services.json` sense opcions), es torna a provar a la crida següent. */
  val app: FirebaseApp by lazy {
    val app =
      FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
        ?: FirebaseApp.initializeApp(
          context,
          checkNotNull(FirebaseOptions.fromResource(context)) { "No Firebase options in the resources" },
          APP_NAME,
        )
    // Abans de qualsevol crida d'Auth o de Firestore: amb App Check forçat, sense atestació no hi ha ni sessió
    FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(appCheckProvider())
    app
  }

  val auth: FirebaseAuth get() = FirebaseAuth.getInstance(app)

  val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance(app)

  private companion object {
    const val APP_NAME = "sync"
  }
}

/**
 * Sense connexió o el servidor no respon: es pot tornar a provar i no és cap error per registrar. Es mira tota la
 * cadena de causes, perquè Firebase sovint embolcalla l'error de xarxa.
 */
internal fun Throwable.isNetworkFailure(): Boolean =
  generateSequence(this) { it.cause }.take(MAX_CAUSES).any { error ->
    when (error) {
      is FirebaseNetworkException, is UnknownHostException, is ConnectException, is SocketTimeoutException -> true
      is FirebaseFirestoreException ->
        error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
          error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
      else -> false
    }
  }

/** Les regles de Firestore han rebutjat l'escriptura. Es mira tota la cadena de causes, com a [isNetworkFailure]. */
internal fun Throwable.isPermissionDenied(): Boolean =
  generateSequence(this) { it.cause }.take(MAX_CAUSES).any { error ->
    error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
  }

// Una cadena de causes pot ser circular
private const val MAX_CAUSES = 10

/** Espera una `Task` de Play Services sense afegir `kotlinx-coroutines-play-services` només per a això. */
internal suspend fun <T> Task<T>.awaitTask(): T =
  suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
      val error = task.exception
      when {
        task.isCanceled -> continuation.cancel()
        error != null -> continuation.resumeWithException(error)
        else -> {
          @Suppress("UNCHECKED_CAST")
          continuation.resume(task.result as T)
        }
      }
    }
  }

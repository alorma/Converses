package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.flags.FeatureFlag
import com.alorma.contactnotes.data.flags.FeatureFlagRunner
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import kotlinx.coroutines.flow.Flow

/**
 * La sincronització al núvol (`docs/sync/disseny.md`). Apagat, l'app és la de sempre: sense indicador, sense fila a
 * Ajustos i sense inicialitzar la instància `sync` de Firebase.
 */
class SyncFlag(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "sync")

/** El compte de Google amb què se sincronitza. Només serveix per saber de qui són les dades: no pot desxifrar res. */
class SyncAccount(
  val uid: String,
  val name: String?,
  val email: String?,
) {
  override fun equals(other: Any?): Boolean =
    other is SyncAccount && uid == other.uid && name == other.name && email == other.email

  override fun hashCode(): Int = uid.hashCode()

  /** Sense nom ni correu: un compte no ha de sortir mai en un log ni en un missatge d'error. */
  override fun toString(): String = "SyncAccount"
}

/**
 * La sessió de Firebase Auth sobre la instància `sync`. Cridar qualsevol dels seus mètodes inicialitza la instància, i
 * per això només s'hi arriba quan el dispositiu té una sessió marcada ([SyncLocalStore]) o l'usuari en comença una.
 */
interface AuthGateway {
  /** La sessió que Firebase Auth conserva al dispositiu, sense connexió; `null` si no n'hi ha. */
  suspend fun currentAccount(): SyncAccount?

  /** El full «Continua amb Google» de Credential Manager i, amb el seu token, la sessió de Firebase Auth. */
  suspend fun signIn(activity: Activity): SignInResult

  suspend fun signOut()
}

sealed interface SignInResult {
  data class Success(
    val account: SyncAccount,
  ) : SignInResult

  /** L'usuari ha tancat el full. No és cap error. */
  data object Cancelled : SignInResult

  data object Offline : SignInResult

  data object Failed : SignInResult
}

/** El que la fase 3 desa a `users/{uid}`: la clau de dades embolcallada amb el secret, i la versió del format. */
class RemoteAccount(
  val wrappedKey: ByteArray,
  val format: Int,
)

sealed interface RemoteResult<out T> {
  data class Ok<T>(
    val value: T,
  ) : RemoteResult<T>

  /** Sense connexió o el servidor no respon: es pot tornar a provar. */
  data object Offline : RemoteResult<Nothing>

  data object Failed : RemoteResult<Nothing>
}

sealed interface CreateResult {
  data object Created : CreateResult

  /** Un altre dispositiu ha activat el compte mentrestant: la seva clau és la bona. */
  data object AlreadyExists : CreateResult

  data object Offline : CreateResult

  data object Failed : CreateResult
}

/** El document del compte a Firestore. */
interface AccountRemote {
  /** Sempre del servidor, mai de la memòria cau. `Ok(null)`: el compte encara no té dades. */
  suspend fun load(uid: String): RemoteResult<RemoteAccount?>

  /** Crea el document només si no existeix. */
  suspend fun create(
    uid: String,
    account: RemoteAccount,
  ): CreateResult
}

/** El secret i la clau de dades d'un compte, tal com es guarden al dispositiu. */
class StoredKeys(
  val uid: String,
  val secret: RecoverySecret,
  val dataKey: DataKey,
) {
  override fun toString(): String = "StoredKeys"
}

/**
 * El fitxer amb la clau, xifrat amb una clau de l'Android Keystore i fora de la còpia de seguretat. No sobreviu a un
 * factory reset, a una desinstal·lació ni a un «esborra dades»: llavors calen les paraules.
 */
interface SyncKeyStore {
  /** `null` si no hi ha clau o el fitxer ja no es pot obrir. */
  fun load(): StoredKeys?

  fun save(keys: StoredKeys)

  fun clear()
}

/** El que la sincronització recorda d'aquest dispositiu sense haver d'inicialitzar Firebase. */
interface SyncLocalStore {
  /** Quan es van obrir les paraules de seguretat per última vegada en aquest dispositiu; `null` sense sessió. */
  val wordsReviewedAt: Flow<Long?>

  /** Si aquest dispositiu ha iniciat sessió per sincronitzar. És el que decideix si Firebase `sync` arrenca. */
  suspend fun hasSession(): Boolean

  /** Amb `false` també s'oblida [wordsReviewedAt]. */
  suspend fun setSession(active: Boolean)

  suspend fun setWordsReviewedAt(millis: Long)
}

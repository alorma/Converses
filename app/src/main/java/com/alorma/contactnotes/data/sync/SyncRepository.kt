package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.Bip39Result
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncState {
  /** Sense sessió: l'app funciona com sempre i res surt del dispositiu. */
  data object SignedOut : SyncState

  /** Recuperant la sessió en arrencar o iniciant-la. */
  data object Checking : SyncState

  /**
   * Primer dispositiu: sessió iniciada i paraules generades, però res pujat. Fins a [SyncRepository.confirmActivation]
   * el compte no té cap dada al núvol.
   */
  data class Activating(
    val account: SyncAccount,
  ) : SyncState

  /** El compte ja té dades i aquest dispositiu no en té la clau: calen les paraules. */
  data class NeedsKey(
    val account: SyncAccount,
  ) : SyncState

  /** Sessió iniciada i clau al dispositiu. */
  data class Ready(
    val account: SyncAccount,
  ) : SyncState
}

enum class SignInOutcome { Done, Cancelled, Offline, Failed }

enum class ActivationOutcome {
  Done,

  /** Un altre dispositiu ha activat el compte mentrestant; aquest passa a [SyncState.NeedsKey]. */
  AlreadyActive,
  Offline,
  Failed,
}

sealed interface WordsOutcome {
  data object Accepted : WordsOutcome

  /** No són 12 paraules vàlides; [problem] diu què hi falla. Mai és [Bip39Result.Valid]. */
  data class Invalid(
    val problem: Bip39Result,
  ) : WordsOutcome

  /** Són paraules vàlides, però no obren les dades d'aquest compte. */
  data object Wrong : WordsOutcome

  data object Offline : WordsOutcome

  data object Failed : WordsOutcome
}

/**
 * El compte i la clau de la sincronització. La resta de l'app no sap res de Firebase: mira [state] i crida les accions.
 * Encara no puja ni baixa cap registre (fase 4).
 */
class SyncRepository(
  private val auth: AuthGateway,
  private val remote: AccountRemote,
  private val keyStore: SyncKeyStore,
  private val local: SyncLocalStore,
  private val scope: CoroutineScope,
  private val clock: () -> Long = System::currentTimeMillis,
  private val random: SecureRandom = SecureRandom(),
) {
  private val mutex = Mutex()
  private val mutableState = MutableStateFlow<SyncState>(SyncState.SignedOut)

  /** Les claus acabades de generar a [SyncState.Activating]; només viuen en memòria fins que es confirmen. */
  private var pending: StoredKeys? = null
  private var keys: StoredKeys? = null
  private var wordsUnlocked = false

  val state: StateFlow<SyncState> = mutableState.asStateFlow()

  val wordsReviewedAt: Flow<Long?> get() = local.wordsReviewedAt

  /**
   * Recupera la sessió d'aquest dispositiu, si en té. Només es crida amb [SyncFlag] encès. Sense sessió marcada no toca
   * Firebase.
   */
  fun start() {
    mutableState.value = SyncState.Checking
    scope.launch { mutex.withLock { mutableState.value = restore() } }
  }

  private suspend fun restore(): SyncState {
    if (!local.hasSession()) return SyncState.SignedOut
    val account = auth.currentAccount() ?: return signedOut()
    val stored = keyStore.load()?.takeIf { it.uid == account.uid }
    if (stored != null) {
      keys = stored
      return SyncState.Ready(account)
    }
    // Sense clau: o bé el compte té dades i calen les paraules, o bé el procés va morir a mitja activació i al núvol
    // no hi ha res. Sense connexió no es pot saber; es resol en donar les paraules
    return when (val loaded = remote.load(account.uid)) {
      is RemoteResult.Ok -> if (loaded.value == null) signedOut() else SyncState.NeedsKey(account)
      RemoteResult.Offline, RemoteResult.Failed -> SyncState.NeedsKey(account)
    }
  }

  suspend fun signIn(activity: Activity): SignInOutcome =
    mutex.withLock {
      if (mutableState.value != SyncState.SignedOut) return SignInOutcome.Failed
      mutableState.value = SyncState.Checking
      val account =
        when (val result = auth.signIn(activity)) {
          is SignInResult.Success -> result.account
          SignInResult.Cancelled -> return backToSignedOut(SignInOutcome.Cancelled)
          SignInResult.Offline -> return backToSignedOut(SignInOutcome.Offline)
          SignInResult.Failed -> return backToSignedOut(SignInOutcome.Failed)
        }
      val remoteAccount =
        when (val loaded = remote.load(account.uid)) {
          is RemoteResult.Ok -> loaded.value
          RemoteResult.Offline -> return backToSignedOut(SignInOutcome.Offline)
          RemoteResult.Failed -> return backToSignedOut(SignInOutcome.Failed)
        }
      local.setSession(true)
      mutableState.value =
        if (remoteAccount == null) {
          pending = StoredKeys(account.uid, RecoverySecret.generate(random), DataKey.generate(random))
          SyncState.Activating(account)
        } else {
          // Un dispositiu que havia tancat la sessió ja no té la clau: sempre calen les paraules
          keyStore.clear()
          SyncState.NeedsKey(account)
        }
      SignInOutcome.Done
    }

  private suspend fun backToSignedOut(outcome: SignInOutcome): SignInOutcome {
    mutableState.value = signedOut()
    return outcome
  }

  /** Després de l'empremta: a partir d'aquí [securityWords] les dona, fins a [lockWords]. */
  fun unlockWords() {
    wordsUnlocked = true
  }

  fun lockWords() {
    wordsUnlocked = false
  }

  /**
   * Les 12 paraules, a [SyncState.Activating] i a [SyncState.Ready]. `null` si no s'han desbloquejat amb
   * [unlockWords]: un procés restaurat a la pantalla de les paraules no les ensenya sense tornar a demanar l'empremta.
   */
  fun securityWords(): List<String>? {
    if (!wordsUnlocked) return null
    return when (mutableState.value) {
      is SyncState.Activating -> pending?.secret?.words
      is SyncState.Ready -> keys?.secret?.words
      else -> null
    }
  }

  /** «Les he guardat en un lloc segur» i «Continua»: puja la clau embolcallada i la desa al dispositiu. */
  suspend fun confirmActivation(): ActivationOutcome =
    mutex.withLock {
      val current = mutableState.value as? SyncState.Activating ?: return ActivationOutcome.Failed
      val generated = pending ?: return ActivationOutcome.Failed
      val wrapped = KeyWrap.wrap(generated.dataKey, generated.secret, current.account.uid, random)
      when (remote.create(current.account.uid, RemoteAccount(wrapped, SYNC_FORMAT))) {
        CreateResult.Created -> {
          keyStore.save(generated)
          keys = generated
          pending = null
          local.setWordsReviewedAt(clock())
          mutableState.value = SyncState.Ready(current.account)
          ActivationOutcome.Done
        }
        CreateResult.AlreadyExists -> {
          pending = null
          wordsUnlocked = false
          mutableState.value = SyncState.NeedsKey(current.account)
          ActivationOutcome.AlreadyActive
        }
        CreateResult.Offline -> ActivationOutcome.Offline
        CreateResult.Failed -> ActivationOutcome.Failed
      }
    }

  /**
   * Tancar les paraules abans de continuar: es tanca la sessió i al núvol no hi queda res. `true` si hi havia una
   * activació a mitges; `false` si ja s'havia confirmat o cancel·lat.
   */
  suspend fun cancelActivation(): Boolean =
    mutex.withLock {
      if (mutableState.value !is SyncState.Activating) return false
      mutableState.value = signedOut()
      true
    }

  /** Les paraules escrites en un dispositiu nou. Si obren la clau del compte, es desa i el dispositiu queda llest. */
  suspend fun submitWords(words: List<String>): WordsOutcome =
    mutex.withLock {
      val current = mutableState.value as? SyncState.NeedsKey ?: return WordsOutcome.Failed
      val secret =
        when (val decoded = Bip39.decode(words)) {
          is Bip39Result.Valid -> RecoverySecret(decoded.entropy)
          else -> return WordsOutcome.Invalid(decoded)
        }
      val remoteAccount =
        when (val loaded = remote.load(current.account.uid)) {
          is RemoteResult.Ok -> loaded.value
          RemoteResult.Offline -> return WordsOutcome.Offline
          RemoteResult.Failed -> return WordsOutcome.Failed
        }
      if (remoteAccount == null) {
        // El compte no té dades (una activació que no es va acabar): no hi ha res per obrir
        mutableState.value = signedOut()
        return WordsOutcome.Failed
      }
      val dataKey =
        KeyWrap.unwrap(remoteAccount.wrappedKey, secret, current.account.uid, remoteAccount.format)
          ?: return WordsOutcome.Wrong
      val opened = StoredKeys(current.account.uid, secret, dataKey)
      keyStore.save(opened)
      keys = opened
      local.setWordsReviewedAt(clock())
      mutableState.value = SyncState.Ready(current.account)
      WordsOutcome.Accepted
    }

  /** Obrir les paraules reinicia el comptador de «Les vas revisar fa…». */
  suspend fun wordsReviewed() {
    if (mutableState.value is SyncState.Ready) local.setWordsReviewedAt(clock())
  }

  /** Les dades es queden al dispositiu; se'n va la sessió i la clau. */
  suspend fun signOut() {
    mutex.withLock {
      if (mutableState.value != SyncState.SignedOut) mutableState.value = signedOut()
    }
  }

  private suspend fun signedOut(): SyncState {
    pending = null
    keys = null
    wordsUnlocked = false
    auth.signOut()
    keyStore.clear()
    local.setSession(false)
    return SyncState.SignedOut
  }
}

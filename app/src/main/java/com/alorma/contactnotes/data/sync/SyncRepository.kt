package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.Bip39Result
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
 * Les dues operacions del motor. Una interfície perquè el repositori es pugui provar sense el motor de debò.
 */
interface SyncRunner {
  suspend fun push(
    uid: String,
    cipher: RecordCipher,
  ): SyncOutcome

  suspend fun pull(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit,
  ): SyncOutcome
}

class EngineSyncRunner(
  private val engine: SyncEngine,
) : SyncRunner {
  override suspend fun push(
    uid: String,
    cipher: RecordCipher,
  ): SyncOutcome = engine.pushPending(uid, cipher)

  override suspend fun pull(
    uid: String,
    cipher: RecordCipher,
    onProgress: (done: Int, total: Int?) -> Unit,
  ): SyncOutcome = engine.pullAll(uid, cipher, onProgress)
}

/**
 * Tot el que el repositori necessita per moure registres. `null` al repositori vol dir «sense motor» (tests de només
 * compte). [deviceName] i [isPro] són funcions perquè el repositori no sàpiga res d'Android ni de RevenueCat.
 */
class SyncData(
  val runner: SyncRunner,
  val source: LocalSyncSource,
  val records: SyncRemote,
  val cursors: SyncCursorStore,
  val deviceName: () -> String,
  val isPro: () -> Boolean,
)

/**
 * La pujada o baixada de després d'activar un dispositiu, perquè la pantalla la pugui seguir ([progress] de 0 a 1, o
 * `null` si no es pot mesurar). Mai porta noms ni contingut.
 */
sealed interface InitialSync {
  /** Primer dispositiu: pujant-ho tot. */
  data class Uploading(
    val progress: Float?,
  ) : InitialSync

  /** Dispositiu nou: baixant-ho tot. */
  data class Downloading(
    val progress: Float?,
  ) : InitialSync

  /** Aquest dispositiu ja té [entryCount] entrades. Només informa: [SyncRepository.continueMerge] puja i baixa. */
  data class MergePrompt(
    val entryCount: Int,
  ) : InitialSync

  data object Done : InitialSync

  /** Sense connexió; [SyncRepository.retryInitialSync] ho torna a provar. */
  data object Offline : InitialSync

  data object Failed : InitialSync
}

/** El que el bucle de sincronització (`SyncLoop`) demana al repositori. */
interface SyncActions {
  suspend fun push(): SyncOutcome

  suspend fun pull(): SyncOutcome

  /** Nombre de canvis locals pendents de pujar. */
  val pendingCount: Flow<Int>

  /** Emet quan algun altre dispositiu ha escrit; s'acaba o falla si el listener cau. */
  fun remoteChanges(): Flow<Unit>
}

/**
 * El compte i la clau de la sincronització, i el motor quan el dispositiu és [SyncState.Ready]. La resta de l'app no
 * sap res de Firebase: mira [state], [status] i [initialSync] i crida les accions. Quan i amb quin ritme es
 * sincronitza ho decideix el `SyncScheduler`.
 */
class SyncRepository(
  private val auth: AuthGateway,
  private val remote: AccountRemote,
  private val keyStore: SyncKeyStore,
  private val local: SyncLocalStore,
  private val scope: CoroutineScope,
  private val clock: () -> Long = System::currentTimeMillis,
  private val random: SecureRandom = SecureRandom(),
  private val sync: SyncData? = null,
) : SyncActions {
  private val mutex = Mutex()
  private val mutableState = MutableStateFlow<SyncState>(SyncState.SignedOut)

  /** Les claus acabades de generar a [SyncState.Activating]; només viuen en memòria fins que es confirmen. */
  private var pending: StoredKeys? = null
  private var keys: StoredKeys? = null
  private var wordsUnlocked = false

  /** El xifrador del compte actiu; només existeix mentre és [SyncState.Ready]. */
  private class Session(
    val uid: String,
    val cipher: RecordCipher,
  )

  @Volatile private var session: Session? = null

  /** Serialitza les pujades i baixades: dues a la vegada es trepitjarien el cursor i els `dirty`. */
  private val syncMutex = Mutex()
  private var registered = false
  private var readyJob: Job? = null
  private var initialPlan: InitialPlan? = null

  private enum class InitialPlan { Upload, Download, Merge }

  private enum class Entry { Restored, Activated, JoinedDevice }

  private val mutableStatus = MutableStateFlow<SyncStatus>(SyncStatus.UpToDate(null))
  private val mutableInitial = MutableStateFlow<InitialSync?>(null)

  val state: StateFlow<SyncState> = mutableState.asStateFlow()

  /**
   * Com va la sincronització de dades: [SyncStatus.Syncing] mentre corre, [SyncStatus.UpToDate] en acabar bé (l'hora
   * només és en memòria: un procés nou comença amb `null` fins a la primera sincronització), [SyncStatus.Offline] amb els
   * canvis pendents, o [SyncStatus.Failed].
   */
  val status: StateFlow<SyncStatus> = mutableStatus.asStateFlow()

  /**
   * La pujada, fusió i baixada de després d'activar o d'entrar les paraules; `null` si no hi ha res en curs.
   * Primer dispositiu: [InitialSync.Uploading] i [InitialSync.Done]. Dispositiu nou: [InitialSync.Downloading] i
   * [InitialSync.Done]; si ja té entrades, abans [InitialSync.MergePrompt] i s'espera [continueMerge]. [InitialSync.Done],
   * [InitialSync.Offline] i [InitialSync.Failed] es queden fins a [dismissInitialSync]. «Amaga» no cal aturar res: la
   * feina és del repositori, no de la pantalla.
   */
  val initialSync: StateFlow<InitialSync?> = mutableInitial.asStateFlow()

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
      onReady(account, stored, Entry.Restored)
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
          onReady(current.account, generated, Entry.Activated)
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
      onReady(current.account, opened, Entry.JoinedDevice)
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

  // --- Motor ---

  /** Puja el que és pendent. Sense sessió o sense motor no fa res. */
  override suspend fun push(): SyncOutcome = tracked { s, _ -> checkNotNull(sync).runner.push(s.uid, s.cipher) }

  /** Baixa els canvis dels altres dispositius. */
  override suspend fun pull(): SyncOutcome =
    tracked { s, _ -> checkNotNull(sync).runner.pull(s.uid, s.cipher) { _, _ -> } }

  /** Puja i després baixa; és el que fa el worker de fons. S'atura al primer pas que no acaba bé. */
  suspend fun syncNow(): SyncOutcome {
    val pushed = push()
    return if (pushed == SyncOutcome.Done) pull() else pushed
  }

  override val pendingCount: Flow<Int> get() = sync?.source?.observePendingCount() ?: flowOf(0)

  override fun remoteChanges(): Flow<Unit> =
    flow {
      val data = sync ?: return@flow
      val current = session ?: return@flow
      emitAll(data.records.listen(current.uid, data.cursors.cursor()).map { })
    }

  /** «Continua» al diàleg de fusió: la fusió és la unió, així que es puja el que hi ha aquí i es baixa tot. */
  fun continueMerge() {
    if (mutableInitial.value !is InitialSync.MergePrompt) return
    runInitial(InitialPlan.Merge)
  }

  /** Torna a provar la pujada o baixada inicial després de [InitialSync.Offline] o [InitialSync.Failed]. */
  fun retryInitialSync() {
    val plan = initialPlan ?: return
    if (mutableInitial.value is InitialSync.Offline || mutableInitial.value is InitialSync.Failed) runInitial(plan)
  }

  /** La pantalla ha acabat: ja no hi ha res a seguir. */
  fun dismissInitialSync() {
    mutableInitial.value = null
  }

  private fun onReady(
    account: SyncAccount,
    stored: StoredKeys,
    entry: Entry,
  ) {
    val data = sync ?: return
    session = Session(account.uid, RecordCipher(stored.dataKey, account.uid))
    readyJob?.cancel()
    readyJob =
      scope.launch {
        when (entry) {
          Entry.Restored -> tracked { _, _ -> SyncOutcome.Done }
          Entry.Activated -> runInitialNow(InitialPlan.Upload)
          Entry.JoinedDevice -> {
            val count = data.source.entryCount()
            if (count > 0) {
              initialPlan = InitialPlan.Merge
              mutableInitial.value = InitialSync.MergePrompt(count)
            } else {
              runInitialNow(InitialPlan.Download)
            }
          }
        }
      }
  }

  private fun runInitial(plan: InitialPlan) {
    readyJob?.cancel()
    readyJob = scope.launch { runInitialNow(plan) }
  }

  private suspend fun runInitialNow(plan: InitialPlan) {
    initialPlan = plan
    var outcome = SyncOutcome.Done
    if (plan != InitialPlan.Download) outcome = upload()
    if (plan != InitialPlan.Upload && outcome == SyncOutcome.Done) outcome = download()
    if (session == null) return
    mutableInitial.value =
      when (outcome) {
        SyncOutcome.Done -> InitialSync.Done
        SyncOutcome.Offline -> InitialSync.Offline
        SyncOutcome.Failed -> InitialSync.Failed
      }
  }

  /** El progrés surt de quants pendents queden: cada fila pujada en treu un. */
  private suspend fun upload(): SyncOutcome {
    mutableInitial.value = InitialSync.Uploading(0f)
    return tracked { s, report ->
      val data = checkNotNull(sync)
      coroutineScope {
        val total = data.source.observePendingCount().first()
        val watcher =
          launch {
            data.source.observePendingCount().collect { left ->
              if (total > 0) report(((total - left).toFloat() / total).coerceIn(0f, 1f))
            }
          }
        try {
          data.runner.push(s.uid, s.cipher)
        } finally {
          watcher.cancel()
        }
      }
    }
  }

  private suspend fun download(): SyncOutcome {
    mutableInitial.value = InitialSync.Downloading(null)
    return tracked { s, report ->
      checkNotNull(sync).runner.pull(s.uid, s.cipher) { done, total ->
        report(total?.takeIf { it > 0 }?.let { (done.toFloat() / it).coerceIn(0f, 1f) })
      }
    }
  }

  /**
   * Una operació del motor, d'una en una, amb el dispositiu ja apuntat i l'[status] al dia. [report] actualitza el
   * progrés a l'estat i, si hi ha una pujada o baixada inicial en curs, també a [initialSync]. Cancel·lada, l'estat
   * torna a ser el d'abans: no es pot quedar «Sincronitzant» per sempre.
   */
  private suspend fun tracked(block: suspend (Session, (Float?) -> Unit) -> SyncOutcome): SyncOutcome {
    if (sync == null) return SyncOutcome.Done
    return syncMutex.withLock {
      val current = session ?: return@withLock SyncOutcome.Done
      val before = mutableStatus.value
      mutableStatus.value = SyncStatus.Syncing(null)
      val report: (Float?) -> Unit = { progress ->
        mutableStatus.value = SyncStatus.Syncing(progress)
        when (mutableInitial.value) {
          is InitialSync.Uploading -> mutableInitial.value = InitialSync.Uploading(progress)
          is InitialSync.Downloading -> mutableInitial.value = InitialSync.Downloading(progress)
          else -> Unit
        }
      }
      val outcome =
        try {
          registerDevice(current).takeIf { it != SyncOutcome.Done } ?: block(current, report)
        } catch (e: CancellationException) {
          mutableStatus.value = before
          throw e
        }
      if (session != null) {
        mutableStatus.value =
          when (outcome) {
            SyncOutcome.Done -> SyncStatus.UpToDate(clock())
            SyncOutcome.Offline -> SyncStatus.Offline(pendingNow())
            SyncOutcome.Failed -> SyncStatus.Failed
          }
      }
      outcome
    }
  }

  private suspend fun pendingNow(): Int = sync?.source?.observePendingCount()?.first() ?: 0

  /**
   * Apunta aquest dispositiu a `devices` (una vegada per sessió). Les regles exigeixen que qui escriu hi sigui, així
   * que va abans de tota pujada. El nom va xifrat amb un identificador fix, com qualsevol registre.
   */
  private suspend fun registerDevice(current: Session): SyncOutcome {
    if (registered) return SyncOutcome.Done
    val data = checkNotNull(sync)
    val deviceId = data.cursors.deviceId()
    val sealedName = current.cipher.encrypt("device-name:$deviceId", data.deviceName().toByteArray(Charsets.UTF_8))
    val result = data.records.registerDevice(current.uid, RemoteDevice(deviceId, sealedName, data.isPro()))
    return when (result) {
      is RemoteResult.Ok -> {
        registered = true
        SyncOutcome.Done
      }
      RemoteResult.Offline -> SyncOutcome.Offline
      RemoteResult.Failed -> SyncOutcome.Failed
    }
  }

  private suspend fun signedOut(): SyncState {
    readyJob?.cancel()
    readyJob = null
    session = null
    registered = false
    initialPlan = null
    mutableInitial.value = null
    mutableStatus.value = SyncStatus.UpToDate(null)
    pending = null
    keys = null
    wordsUnlocked = false
    auth.signOut()
    keyStore.clear()
    local.setSession(false)
    return SyncState.SignedOut
  }
}

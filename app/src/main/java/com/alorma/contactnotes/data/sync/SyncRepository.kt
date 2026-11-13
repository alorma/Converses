package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.Bip39Result
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecordCipher
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import com.alorma.contactnotes.data.tracking.ErrorSites
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

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

  /** Un altre dispositiu ha tret aquest de la llista: té la clau i les dades, però ja no sincronitza. */
  data class Removed(
    val account: SyncAccount,
  ) : SyncState
}

/** Una fila de la llista de dispositius, amb el nom ja desxifrat ([name] és `null` si no s'ha pogut llegir). */
data class DeviceInfo(
  val id: String,
  val name: String?,
  val addedAt: Long?,
  val lastSyncAt: Long?,
  val isThis: Boolean,
) {
  /** Sense el nom: un dispositiu no ha de sortir mai en un log. */
  override fun toString(): String = "DeviceInfo"
}

/**
 * Les places són plenes en apuntar aquest dispositiu: cal treure'n un o no continuar. [devices] són els que hi ha
 * (sense aquest) i [canUpgrade] diu si passar a Pro en donaria més.
 */
data class SlotsPrompt(
  val limit: Int,
  val devices: List<DeviceInfo>,
  val canUpgrade: Boolean,
)

enum class DeviceActionOutcome { Done, Offline, Failed }

enum class RejoinOutcome { Done, Full, Offline, Failed }

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
  val devices: DeviceStore,
  val deviceName: () -> String,
  val isPro: () -> Boolean,
  /** Cada canvi d'`isPro` (un `StateFlow`: el primer valor és l'actual); manté `plan` al dia mentre és `Ready`. */
  val isProChanges: Flow<Boolean> = emptyFlow(),
  /** Rep els errors (només l'error i el lloc, mai noms ni dades). */
  val onError: (Throwable, String) -> Unit = { _, _ -> },
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

  /**
   * Mira `devices` mentre l'app és visible i passa a [SyncState.Removed] si aquest dispositiu ja no hi és. No acaba
   * mai si no hi ha res a mirar.
   */
  suspend fun watchDevices() {
    awaitCancellation()
  }
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
  private var readyJob: Job? = null
  private var initialPlan: InitialPlan? = null

  /** «Amaga» a la pujada o baixada inicial: la feina segueix, però la pantalla ja no la mostra. */
  @Volatile private var initialHidden = false

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

  // --- Dispositius i places ---

  /** El xifrador del compte per als dispositius: també existeix a [SyncState.Removed], on el motor ja no corre. */
  private val deviceView = MutableStateFlow<Session?>(null)
  private val mutableSlots = MutableStateFlow<SlotsPrompt?>(null)

  /** Els dispositius del compte, amb aquest marcat. Buit sense sessió. */
  val devices: Flow<List<DeviceInfo>> =
    deviceView.flatMapLatest { view ->
      val data = sync
      if (view == null || data == null) {
        flowOf(emptyList())
      } else {
        data.records.observeDevices(view.uid).map { remoteDevices ->
          val thisId = data.devices.deviceId()
          remoteDevices.entries.map { toInfo(it, view.cipher, thisId) }
        }
      }
    }

  /** Quants dispositius admet el pla actual (el `plan` del document). */
  val deviceLimit: Flow<Int> =
    deviceView.flatMapLatest { view ->
      val data = sync
      if (view == null || data == null) {
        flowOf(FREE_DEVICES)
      } else {
        data.records.observeDevices(view.uid).map { limitOf(it.pro) }
      }
    }

  /** Si no és `null`, les places són plenes i la pantalla ha de demanar quin dispositiu es treu. No n'hi ha cap de triat. */
  val slotsPrompt: StateFlow<SlotsPrompt?> = mutableSlots.asStateFlow()

  /** Aquest dispositiu s'ha apuntat (o s'ha vist que ja hi era) en aquesta sessió; només llavors el pot treure algú. */
  @Volatile private var joined = false

  /** Primer dispositiu del compte (activació): no cal mirar `devices`. */
  @Volatile private var firstDevice = false

  /** Les places s'han demanat en entrar amb les paraules: en resoldre-les falta fer la fusió i la baixada. */
  private var awaitingJoin = false
  private var lastTouch: Long? = null
  private var lastPlan: Boolean? = null
  private var planJob: Job? = null

  private class UnreadableDeviceNameException : Exception()

  private fun limitOf(pro: Boolean): Int = if (pro) PRO_DEVICES else FREE_DEVICES

  private fun toInfo(
    entry: RemoteDeviceEntry,
    cipher: RecordCipher,
    thisId: String,
  ): DeviceInfo {
    val name = entry.sealedName?.let { cipher.decrypt(deviceNameAad(entry.id), it) }?.decodeToString()
    if (name == null) sync?.onError?.invoke(UnreadableDeviceNameException(), ErrorSites.SYNC_RECORD)
    return DeviceInfo(entry.id, name, entry.addedAt, entry.lastSyncAt, isThis = entry.id == thisId)
  }

  private fun deviceNameAad(id: String) = "device-name:$id"

  /** Treu [id] i continua apuntant aquest dispositiu, en una sola transacció. */
  suspend fun chooseDeviceToRemove(id: String): DeviceActionOutcome =
    mutex.withLock {
      val prompt = mutableSlots.value ?: return DeviceActionOutcome.Failed
      if (prompt.devices.none { it.id == id }) return DeviceActionOutcome.Failed
      val state = mutableState.value
      val account =
        when (state) {
          is SyncState.Ready -> state.account
          is SyncState.Removed -> state.account
          else -> return DeviceActionOutcome.Failed
        }
      val view = deviceView.value ?: return DeviceActionOutcome.Failed
      when (joinWith(view, removing = id)) {
        JoinResult.Joined -> {
          mutableSlots.value = null
          if (state is SyncState.Removed) {
            becomeReady(account, view)
            scope.launch { syncNow() }
          }
          if (awaitingJoin) {
            awaitingJoin = false
            val data = checkNotNull(sync)
            readyJob = scope.launch { afterJoined(data) }
          }
          DeviceActionOutcome.Done
        }
        JoinResult.Full -> {
          reloadSlots(view)
          DeviceActionOutcome.Failed
        }
        JoinResult.Offline -> DeviceActionOutcome.Offline
        JoinResult.Failed -> DeviceActionOutcome.Failed
      }
    }

  /**
   * «Ara no»: tanca la sessió sense deixar res nou al núvol (aquest dispositiu encara no s'hi havia apuntat). Si el
   * dispositiu era a [SyncState.Removed], només es descarta la pregunta: segueix tret, amb les seves dades.
   */
  suspend fun cancelSlots() {
    mutex.withLock {
      mutableSlots.value = null
      awaitingJoin = false
      if (mutableState.value is SyncState.Ready) mutableState.value = signedOut()
    }
  }

  /**
   * «Torna a entrar» d'un dispositiu tret: sense les paraules, que ja les té. Si hi ha plaça s'apunta i fa una pujada i
   * una baixada (la fusió és la unió); si no n'hi ha, [slotsPrompt].
   */
  suspend fun rejoin(): RejoinOutcome =
    mutex.withLock {
      val data = sync ?: return RejoinOutcome.Failed
      val removed = mutableState.value as? SyncState.Removed ?: return RejoinOutcome.Failed
      val view = deviceView.value ?: return RejoinOutcome.Failed
      val loaded =
        when (val result = data.records.loadDevices(view.uid)) {
          is RemoteResult.Ok -> result.value
          RemoteResult.Offline -> return RejoinOutcome.Offline
          RemoteResult.Failed, RemoteResult.Denied -> return RejoinOutcome.Failed
        }
      val deviceId = data.devices.deviceId()
      if (loaded.entries.any { it.id == deviceId }) {
        adopt(loaded, view)
      } else {
        if (loaded.entries.size >= limitOf(loaded.pro || data.isPro())) {
          showSlots(view, loaded)
          return RejoinOutcome.Full
        }
        when (joinWith(view, removing = null)) {
          JoinResult.Joined -> Unit
          JoinResult.Full -> {
            reloadSlots(view)
            return RejoinOutcome.Full
          }
          JoinResult.Offline -> return RejoinOutcome.Offline
          JoinResult.Failed -> return RejoinOutcome.Failed
        }
      }
      becomeReady(removed.account, view)
      scope.launch { syncNow() }
      RejoinOutcome.Done
    }

  /** Treu un altre dispositiu de la llista. */
  suspend fun removeDevice(id: String): DeviceActionOutcome {
    val data = sync ?: return DeviceActionOutcome.Failed
    val view = deviceView.value ?: return DeviceActionOutcome.Failed
    return data.records.removeDevice(view.uid, id).toAction()
  }

  /** Canvia el nom d'un dispositiu (es desa xifrat amb la mateixa AAD que en crear-lo). */
  suspend fun renameDevice(
    id: String,
    name: String,
  ): DeviceActionOutcome {
    val data = sync ?: return DeviceActionOutcome.Failed
    val view = deviceView.value ?: return DeviceActionOutcome.Failed
    val sealed = view.cipher.encrypt(deviceNameAad(id), name.toByteArray(Charsets.UTF_8))
    return data.records.renameDevice(view.uid, id, sealed).toAction()
  }

  private fun RemoteResult<Unit>.toAction(): DeviceActionOutcome =
    when (this) {
      is RemoteResult.Ok -> DeviceActionOutcome.Done
      RemoteResult.Offline -> DeviceActionOutcome.Offline
      RemoteResult.Failed, RemoteResult.Denied -> DeviceActionOutcome.Failed
    }

  /** Mira `devices` mentre l'app és visible: si aquest dispositiu hi desapareix, passa a [SyncState.Removed]. */
  override suspend fun watchDevices() {
    val data = sync
    val view = session
    if (data == null || view == null) awaitCancellation()
    val thisId = data.devices.deviceId()
    data.records.observeDevices(view.uid).collect { seen ->
      // Només el servidor: la memòria cau pot ser d'abans d'apuntar-se (p. ex. just després de «Torna a entrar»)
      if (seen.fromCache) return@collect
      if (!joined || mutableState.value !is SyncState.Ready || seen.entries.any { it.id == thisId }) return@collect
      // Un listener nou pot donar una còpia d'abans de la transacció d'apuntar-se encara que no vingui de la memòria
      // cau (un altre listener del mateix document la manté viva): es confirma amb una lectura del servidor
      val confirmed = (data.records.loadDevices(view.uid) as? RemoteResult.Ok)?.value ?: return@collect
      if (joined && mutableState.value is SyncState.Ready && confirmed.entries.none { it.id == thisId }) enterRemoved()
    }
  }

  /** El motor s'atura (sense sessió, per tant sense pujades ni baixades), però es conserva la clau. */
  private fun enterRemoved() {
    joined = false
    session = null
    mutableInitial.value = null
    mutableStatus.value = SyncStatus.UpToDate(null)
    mutableState.update { if (it is SyncState.Ready) SyncState.Removed(it.account) else it }
  }

  private fun becomeReady(
    account: SyncAccount,
    view: Session,
  ) {
    session = view
    deviceView.value = view
    mutableSlots.value = null
    mutableState.value = SyncState.Ready(account)
  }

  /** Ja és a `devices`: es recorda i, si el pla ha canviat, s'escriu. */
  private suspend fun adopt(
    loaded: RemoteDevices,
    view: Session,
  ) {
    val data = checkNotNull(sync)
    data.devices.setRegistered(true)
    joined = true
    firstDevice = false
    lastPlan = loaded.pro
    syncPlan(data.isPro())
    if (data.records.touchDevice(view.uid, data.devices.deviceId()) is RemoteResult.Ok) lastTouch = clock()
  }

  /** Apunta aquest dispositiu (treient [removing], si cal) i en recorda l'èxit. */
  private suspend fun joinWith(
    view: Session,
    removing: String?,
  ): JoinResult {
    val data = checkNotNull(sync)
    val deviceId = data.devices.deviceId()
    val sealedName = view.cipher.encrypt(deviceNameAad(deviceId), data.deviceName().toByteArray(Charsets.UTF_8))
    val pro = data.isPro()
    val result = data.records.joinDevice(view.uid, RemoteDevice(deviceId, sealedName, pro), removing)
    if (result == JoinResult.Joined) {
      data.devices.setRegistered(true)
      joined = true
      firstDevice = false
      lastPlan = pro
      lastTouch = clock()
    }
    return result
  }

  private suspend fun showSlots(
    view: Session,
    loaded: RemoteDevices,
  ) {
    val data = checkNotNull(sync)
    val thisId = data.devices.deviceId()
    val pro = data.isPro()
    mutableSlots.value =
      SlotsPrompt(
        limit = limitOf(loaded.pro || pro),
        devices = loaded.entries.filter { it.id != thisId }.map { toInfo(it, view.cipher, thisId) },
        canUpgrade = !pro,
      )
  }

  /** Les regles han dit que no hi ha plaça: es torna a llegir el servidor i es pregunta quin es treu. */
  private suspend fun reloadSlots(view: Session) {
    val data = checkNotNull(sync)
    val loaded = (data.records.loadDevices(view.uid) as? RemoteResult.Ok)?.value ?: return
    showSlots(view, loaded)
  }

  /** Escriu `plan` si el que sap el servidor no és [pro]. */
  private suspend fun syncPlan(pro: Boolean) {
    val data = sync ?: return
    val view = deviceView.value ?: return
    if (!joined || lastPlan == pro) return
    if (data.records.setPlan(view.uid, pro) is RemoteResult.Ok) lastPlan = pro
  }

  /** Després d'una sincronització bona: el `plan` pendent i, com a molt cada 15 minuts, `lastSyncAt`. */
  private suspend fun afterSuccessfulSync() {
    val data = sync ?: return
    val view = session ?: return
    if (!joined) return
    syncPlan(data.isPro())
    val now = clock()
    val last = lastTouch
    if (last == null || now - last >= TOUCH_INTERVAL_MILLIS) {
      if (data.records.touchDevice(view.uid, data.devices.deviceId()) is RemoteResult.Ok) lastTouch = now
    }
  }

  /**
   * Una pujada rebutjada per les regles: es torna a llegir `devices`. Si aquest dispositiu hi és, és un error normal; si
   * no, l'han tret.
   */
  private suspend fun resolveDenied(view: Session): SyncOutcome {
    val data = checkNotNull(sync)
    val loaded = (data.records.loadDevices(view.uid) as? RemoteResult.Ok)?.value ?: return SyncOutcome.Failed
    val deviceId = data.devices.deviceId()
    if (joined && loaded.entries.none { it.id == deviceId }) {
      enterRemoved()
      return SyncOutcome.Denied
    }
    return SyncOutcome.Failed
  }

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
      RemoteResult.Offline, RemoteResult.Failed, RemoteResult.Denied -> SyncState.NeedsKey(account)
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
          RemoteResult.Failed, RemoteResult.Denied -> return backToSignedOut(SignInOutcome.Failed)
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
          RemoteResult.Failed, RemoteResult.Denied -> return WordsOutcome.Failed
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

  /**
   * Les dades es queden al dispositiu; se'n va la sessió i la clau. Abans treu aquest dispositiu de `devices` (amb el
   * temps d'espera de les pujades); si no hi ha connexió o s'esgota, tanca igualment i la plaça es treu des d'un altre
   * dispositiu. A [SyncState.Removed] no hi ha res a treure.
   */
  suspend fun signOut() {
    mutex.withLock {
      val current = mutableState.value
      if (current == SyncState.SignedOut) return
      val data = sync
      val view = deviceView.value
      if (data != null && view != null && current is SyncState.Ready && (joined || data.devices.isRegistered())) {
        // Perquè el listener no ho prengui per una expulsió
        joined = false
        val deviceId = data.devices.deviceId()
        withTimeoutOrNull(SIGN_OUT_TIMEOUT_MILLIS) { data.records.removeDevice(view.uid, deviceId) }
      }
      mutableState.value = signedOut()
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

  /**
   * La pantalla ha acabat, o l'usuari ha tocat «Amaga»: ja no hi ha res a seguir. Si la feina encara corre, continua,
   * però en acabar no torna a ensenyar el resultat; l'estat de 13o ja el diu.
   */
  fun dismissInitialSync() {
    initialHidden = true
    mutableInitial.value = null
  }

  /** El nom d'aquest dispositiu, per a «Sincronització activada»; `null` sense motor. */
  fun deviceName(): String? = sync?.deviceName?.invoke()

  private fun onReady(
    account: SyncAccount,
    stored: StoredKeys,
    entry: Entry,
  ) {
    val data = sync ?: return
    val view = Session(account.uid, RecordCipher(stored.dataKey, account.uid))
    session = view
    deviceView.value = view
    firstDevice = entry == Entry.Activated
    planJob?.cancel()
    planJob = scope.launch { data.isProChanges.collect { syncPlan(it) } }
    readyJob?.cancel()
    readyJob =
      scope.launch {
        when (entry) {
          Entry.Restored -> tracked { _, _ -> SyncOutcome.Done }
          Entry.Activated -> runInitialNow(InitialPlan.Upload)
          Entry.JoinedDevice -> {
            // Les places, abans de la fusió i la baixada
            val registration = tracked { _, _ -> SyncOutcome.Done }
            when {
              registration == SyncOutcome.Done -> afterJoined(data)
              mutableSlots.value != null -> awaitingJoin = true
              mutableState.value is SyncState.Removed || session == null -> Unit
              else -> {
                initialPlan = if (data.source.entryCount() > 0) InitialPlan.Merge else InitialPlan.Download
                mutableInitial.value =
                  if (registration == SyncOutcome.Offline) InitialSync.Offline else InitialSync.Failed
              }
            }
          }
        }
      }
  }

  private suspend fun afterJoined(data: SyncData) {
    val count = data.source.entryCount()
    if (count > 0) {
      initialPlan = InitialPlan.Merge
      mutableInitial.value = InitialSync.MergePrompt(count)
    } else {
      runInitialNow(InitialPlan.Download)
    }
  }

  private fun runInitial(plan: InitialPlan) {
    readyJob?.cancel()
    readyJob = scope.launch { runInitialNow(plan) }
  }

  private suspend fun runInitialNow(plan: InitialPlan) {
    initialPlan = plan
    initialHidden = false
    var outcome = SyncOutcome.Done
    if (plan != InitialPlan.Download) outcome = upload()
    if (plan != InitialPlan.Upload && outcome == SyncOutcome.Done) outcome = download()
    if (session == null || initialHidden) return
    mutableInitial.value =
      when (outcome) {
        SyncOutcome.Done -> InitialSync.Done
        SyncOutcome.Offline -> InitialSync.Offline
        SyncOutcome.Failed -> InitialSync.Failed
        SyncOutcome.Denied -> InitialSync.Failed
      }
  }

  /** El progrés surt de quants pendents queden: cada fila pujada en treu un. */
  private suspend fun upload(): SyncOutcome {
    if (!initialHidden) mutableInitial.value = InitialSync.Uploading(0f)
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
    if (!initialHidden) mutableInitial.value = InitialSync.Downloading(null)
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
      var outcome =
        try {
          val registration = registerDevice(current)
          if (registration != SyncOutcome.Done) {
            registration
          } else {
            block(current, report).also { done ->
              if (done == SyncOutcome.Done) afterSuccessfulSync()
            }
          }
        } catch (e: CancellationException) {
          mutableStatus.value = before
          throw e
        }
      if (outcome == SyncOutcome.Denied && session != null) outcome = resolveDenied(current)
      if (session != null) {
        mutableStatus.value =
          when (outcome) {
            SyncOutcome.Done -> SyncStatus.UpToDate(clock())
            SyncOutcome.Offline -> SyncStatus.Offline(pendingNow())
            SyncOutcome.Failed -> SyncStatus.Failed
            SyncOutcome.Denied -> SyncStatus.Failed
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
    if (joined) return SyncOutcome.Done
    // Esperant que l'usuari triï quin dispositiu es treu: res a fer ni a preguntar al servidor
    if (mutableSlots.value != null) return SyncOutcome.Failed
    val data = checkNotNull(sync)
    if (!firstDevice) {
      val loaded =
        when (val result = data.records.loadDevices(current.uid)) {
          is RemoteResult.Ok -> result.value
          RemoteResult.Offline -> return SyncOutcome.Offline
          RemoteResult.Failed, RemoteResult.Denied -> return SyncOutcome.Failed
        }
      val deviceId = data.devices.deviceId()
      when {
        // Primer es mira si ja hi és, i després el límit
        loaded.entries.any { it.id == deviceId } -> {
          adopt(loaded, current)
          return SyncOutcome.Done
        }
        // S'havia apuntat i ja no hi és: l'han tret. Mai s'hi torna a apuntar sol
        data.devices.isRegistered() -> {
          enterRemoved()
          return SyncOutcome.Failed
        }
        loaded.entries.size >= limitOf(loaded.pro || data.isPro()) -> {
          showSlots(current, loaded)
          return SyncOutcome.Failed
        }
      }
    }
    return when (joinWith(current, removing = null)) {
      JoinResult.Joined -> SyncOutcome.Done
      JoinResult.Full -> {
        reloadSlots(current)
        SyncOutcome.Failed
      }
      JoinResult.Offline -> SyncOutcome.Offline
      JoinResult.Failed -> SyncOutcome.Failed
    }
  }

  private suspend fun signedOut(): SyncState {
    readyJob?.cancel()
    readyJob = null
    planJob?.cancel()
    planJob = null
    session = null
    deviceView.value = null
    mutableSlots.value = null
    joined = false
    firstDevice = false
    awaitingJoin = false
    lastTouch = null
    lastPlan = null
    sync?.devices?.setRegistered(false)
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

  private companion object {
    const val FREE_DEVICES = 2
    const val PRO_DEVICES = 5
    const val TOUCH_INTERVAL_MILLIS = 15 * 60 * 1000L

    /** El mateix temps d'espera que les pujades. */
    const val SIGN_OUT_TIMEOUT_MILLIS = 20_000L
  }
}

package com.alorma.contactnotes.testing

import android.app.Activity
import com.alorma.contactnotes.data.sync.AccountRemote
import com.alorma.contactnotes.data.sync.AuthGateway
import com.alorma.contactnotes.data.sync.CreateResult
import com.alorma.contactnotes.data.sync.RemoteAccount
import com.alorma.contactnotes.data.sync.RemoteResult
import com.alorma.contactnotes.data.sync.SignInResult
import com.alorma.contactnotes.data.sync.StoredKeys
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.data.sync.SyncKeyStore
import com.alorma.contactnotes.data.sync.SyncLocalStore
import com.alorma.contactnotes.data.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

val TestAccount = SyncAccount(uid = "uid-1", name = "Test User", email = "test@example.com")

/** [signInResult] és el que respon el full de Google; la sessió queda a [session], com la conserva Firebase Auth. */
class FakeAuthGateway(
  var session: SyncAccount? = null,
  var signInResult: SignInResult = SignInResult.Success(TestAccount),
) : AuthGateway {
  var signOuts = 0
    private set

  override suspend fun currentAccount(): SyncAccount? = session

  override suspend fun signIn(activity: Activity): SignInResult =
    signInResult.also { if (it is SignInResult.Success) session = it.account }

  override suspend fun signOut() {
    signOuts++
    session = null
  }
}

/** Els documents del compte en memòria. [failure] fa respondre «sense connexió» o «error» a tot. */
class FakeAccountRemote : AccountRemote {
  val accounts = mutableMapOf<String, RemoteAccount>()
  var failure: Failure? = null

  enum class Failure { Offline, Failed }

  override suspend fun load(uid: String): RemoteResult<RemoteAccount?> =
    when (failure) {
      Failure.Offline -> RemoteResult.Offline
      Failure.Failed -> RemoteResult.Failed
      null -> RemoteResult.Ok(accounts[uid])
    }

  override suspend fun create(
    uid: String,
    account: RemoteAccount,
  ): CreateResult =
    when (failure) {
      Failure.Offline -> CreateResult.Offline
      Failure.Failed -> CreateResult.Failed
      null ->
        if (uid in accounts) {
          CreateResult.AlreadyExists
        } else {
          accounts[uid] = account
          CreateResult.Created
        }
    }
}

class FakeSyncKeyStore(
  var stored: StoredKeys? = null,
) : SyncKeyStore {
  override fun load(): StoredKeys? = stored

  override fun save(keys: StoredKeys) {
    stored = keys
  }

  override fun clear() {
    stored = null
  }
}

class FakeSyncLocalStore(
  var session: Boolean = false,
) : SyncLocalStore {
  override val wordsReviewedAt = MutableStateFlow<Long?>(null)

  override suspend fun hasSession(): Boolean = session

  override suspend fun setSession(active: Boolean) {
    session = active
    if (!active) wordsReviewedAt.value = null
  }

  override suspend fun setWordsReviewedAt(millis: Long) {
    wordsReviewedAt.value = millis
  }
}

/** Un [SyncRepository] amb tots els falsos a la vista, per als tests de ViewModel. */
class FakeSyncEnvironment(
  scope: CoroutineScope,
  val auth: FakeAuthGateway = FakeAuthGateway(),
  val remote: FakeAccountRemote = FakeAccountRemote(),
  val keyStore: FakeSyncKeyStore = FakeSyncKeyStore(),
  val local: FakeSyncLocalStore = FakeSyncLocalStore(),
  var now: Long = 1_000L,
) {
  val repository = SyncRepository(auth, remote, keyStore, local, scope, clock = { now })
}

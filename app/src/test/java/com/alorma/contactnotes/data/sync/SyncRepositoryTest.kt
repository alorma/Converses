package com.alorma.contactnotes.data.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.crypto.Bip39Result
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeSyncEnvironment
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryTest {
  private val scope = TestScope(UnconfinedTestDispatcher())
  private val env = FakeSyncEnvironment(scope)
  private val repository = env.repository
  private val activity = Activity()

  /** Un compte que ja té dades al núvol, activat des d'un altre dispositiu. */
  private fun existingAccount(): RecoverySecret {
    val secret = RecoverySecret.generate()
    env.remote.accounts[TestAccount.uid] =
      RemoteAccount(KeyWrap.wrap(DataKey.generate(), secret, TestAccount.uid), SYNC_FORMAT)
    return secret
  }

  @Test
  fun withoutAMarkedSessionStartDoesNotTouchFirebase() =
    scope.runTest {
      env.auth.session = TestAccount
      repository.start()
      assertEquals(SyncState.SignedOut, repository.state.value)
      assertEquals(0, env.auth.signOuts)
    }

  @Test
  fun signingInToANewAccountGeneratesWordsButUploadsNothing() =
    scope.runTest {
      assertEquals(SignInOutcome.Done, repository.signIn(activity))
      assertEquals(SyncState.Activating(TestAccount), repository.state.value)
      assertTrue(env.remote.accounts.isEmpty())
      assertNull(env.keyStore.stored)
    }

  @Test
  fun theWordsNeedUnlockingFirst() =
    scope.runTest {
      repository.signIn(activity)
      assertNull(repository.securityWords())
      repository.unlockWords()
      assertEquals(12, repository.securityWords()?.size)
      repository.lockWords()
      assertNull(repository.securityWords())
    }

  @Test
  fun confirmingUploadsTheWrappedKeyAndStoresItOnTheDevice() =
    scope.runTest {
      repository.signIn(activity)
      repository.unlockWords()
      val words = repository.securityWords()
      env.now = 5_000L
      assertEquals(ActivationOutcome.Done, repository.confirmActivation())
      assertEquals(SyncState.Ready(TestAccount), repository.state.value)

      val uploaded = env.remote.accounts.getValue(TestAccount.uid)
      val stored = env.keyStore.stored
      assertNotNull(stored)
      assertEquals(words, stored?.secret?.words)
      assertEquals(stored?.dataKey, KeyWrap.unwrap(uploaded.wrappedKey, stored!!.secret, TestAccount.uid))
      assertEquals(SYNC_FORMAT, uploaded.format)
      assertEquals(5_000L, env.local.wordsReviewedAt.value)
      assertEquals(words, repository.securityWords())
    }

  @Test
  fun closingTheWordsBeforeContinuingSignsOutAndLeavesNothingInTheCloud() =
    scope.runTest {
      repository.signIn(activity)
      repository.cancelActivation()
      assertEquals(SyncState.SignedOut, repository.state.value)
      assertNull(env.auth.session)
      assertTrue(env.remote.accounts.isEmpty())
      assertFalse(env.local.session)
    }

  @Test
  fun confirmingOfflineStaysInActivation() =
    scope.runTest {
      repository.signIn(activity)
      env.remote.failure = FakeAccountRemote.Failure.Offline
      assertEquals(ActivationOutcome.Offline, repository.confirmActivation())
      assertEquals(SyncState.Activating(TestAccount), repository.state.value)
      env.remote.failure = null
      assertEquals(ActivationOutcome.Done, repository.confirmActivation())
    }

  @Test
  fun anAccountActivatedMeanwhileFromAnotherDeviceAsksForItsWords() =
    scope.runTest {
      repository.signIn(activity)
      existingAccount()
      assertEquals(ActivationOutcome.AlreadyActive, repository.confirmActivation())
      assertEquals(SyncState.NeedsKey(TestAccount), repository.state.value)
      assertNull(env.keyStore.stored)
    }

  @Test
  fun signingInOfflineLeavesNoSession() =
    scope.runTest {
      env.remote.failure = FakeAccountRemote.Failure.Offline
      assertEquals(SignInOutcome.Offline, repository.signIn(activity))
      assertEquals(SyncState.SignedOut, repository.state.value)
      assertNull(env.auth.session)
      assertFalse(env.local.session)
    }

  @Test
  fun cancellingTheGoogleSheetIsNotAnError() =
    scope.runTest {
      env.auth.signInResult = SignInResult.Cancelled
      assertEquals(SignInOutcome.Cancelled, repository.signIn(activity))
      assertEquals(SyncState.SignedOut, repository.state.value)
    }

  @Test
  fun anAccountWithDataNeedsTheWords() =
    scope.runTest {
      val secret = existingAccount()
      repository.signIn(activity)
      assertEquals(SyncState.NeedsKey(TestAccount), repository.state.value)

      assertEquals(WordsOutcome.Accepted, repository.submitWords(secret.words))
      assertEquals(SyncState.Ready(TestAccount), repository.state.value)
      assertEquals(secret, env.keyStore.stored?.secret)
    }

  @Test
  fun wordsFromAnotherAccountDoNotOpenThisOne() =
    scope.runTest {
      existingAccount()
      repository.signIn(activity)
      assertEquals(WordsOutcome.Wrong, repository.submitWords(RecoverySecret.generate().words))
      assertEquals(SyncState.NeedsKey(TestAccount), repository.state.value)
      assertNull(env.keyStore.stored)
    }

  @Test
  fun invalidWordsSayWhatIsWrongWithoutAskingTheServer() =
    scope.runTest {
      val secret = existingAccount()
      repository.signIn(activity)
      env.remote.failure = FakeAccountRemote.Failure.Failed
      val words = secret.words.toMutableList().also { it[2] = "xyz" }
      assertEquals(WordsOutcome.Invalid(Bip39Result.UnknownWords(listOf(2))), repository.submitWords(words))
    }

  @Test
  fun submittingWordsOfflineCanBeRetried() =
    scope.runTest {
      val secret = existingAccount()
      repository.signIn(activity)
      env.remote.failure = FakeAccountRemote.Failure.Offline
      assertEquals(WordsOutcome.Offline, repository.submitWords(secret.words))
      assertEquals(SyncState.NeedsKey(TestAccount), repository.state.value)
    }

  @Test
  fun aDeviceWithItsKeyIsReadyAtStartWithoutConnection() =
    scope.runTest {
      val secret = existingAccount()
      repository.signIn(activity)
      repository.submitWords(secret.words)

      env.remote.failure = FakeAccountRemote.Failure.Offline
      val restarted = FakeSyncEnvironment(scope, env.auth, env.remote, env.keyStore, env.local).repository
      restarted.start()
      assertEquals(SyncState.Ready(TestAccount), restarted.state.value)
      assertNull(restarted.securityWords())
    }

  @Test
  fun aProcessKilledDuringActivationSignsOutAtStart() =
    scope.runTest {
      repository.signIn(activity)
      val restarted = FakeSyncEnvironment(scope, env.auth, env.remote, env.keyStore, env.local).repository
      restarted.start()
      assertEquals(SyncState.SignedOut, restarted.state.value)
      assertNull(env.auth.session)
    }

  @Test
  fun aSessionWithoutKeyNeedsTheWordsAtStart() =
    scope.runTest {
      existingAccount()
      repository.signIn(activity)
      val restarted = FakeSyncEnvironment(scope, env.auth, env.remote, env.keyStore, env.local).repository
      restarted.start()
      assertEquals(SyncState.NeedsKey(TestAccount), restarted.state.value)
    }

  @Test
  fun signingOutRemovesTheSessionAndTheKey() =
    scope.runTest {
      repository.signIn(activity)
      repository.confirmActivation()
      repository.signOut()
      assertEquals(SyncState.SignedOut, repository.state.value)
      assertNull(env.auth.session)
      assertNull(env.keyStore.stored)
      assertFalse(env.local.session)
      assertNull(env.local.wordsReviewedAt.value)
      // El núvol no es toca
      assertTrue(TestAccount.uid in env.remote.accounts)
    }

  @Test
  fun signingInAgainAfterSigningOutAsksForTheWords() =
    scope.runTest {
      repository.signIn(activity)
      repository.unlockWords()
      val words = repository.securityWords()!!
      repository.confirmActivation()
      repository.signOut()

      repository.signIn(activity)
      assertEquals(SyncState.NeedsKey(TestAccount), repository.state.value)
      assertEquals(WordsOutcome.Accepted, repository.submitWords(words))
    }

  @Test
  fun openingTheWordsResetsTheReviewDate() =
    scope.runTest {
      repository.signIn(activity)
      repository.confirmActivation()
      env.now = 9_000L
      repository.wordsReviewed()
      assertEquals(9_000L, env.local.wordsReviewedAt.value)
    }
}

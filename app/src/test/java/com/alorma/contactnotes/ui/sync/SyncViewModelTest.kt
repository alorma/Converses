package com.alorma.contactnotes.ui.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.RemoteAccount
import com.alorma.contactnotes.data.sync.SignInResult
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.testing.FakeSyncEnvironment
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val scope = TestScope(UnconfinedTestDispatcher())
  private val env = FakeSyncEnvironment(scope)
  private val tracker = RecordingTracker()

  // Després de la regla: `viewModelScope` es crea amb el constructor i ha de veure el `Main` de prova
  private val vm by lazy { SyncViewModel(env.repository, tracker.eventTracker) }

  @Test
  fun firstDeviceSignInIsTrackedAsActivating() =
    scope.runTest {
      vm.onSignIn(Activity())
      assertEquals(
        listOf(
          "sync_sign_in_started" to emptyMap(),
          "sync_sign_in_finished" to mapOf("result" to "activating"),
        ),
        tracker.recordedActions(),
      )
    }

  @Test
  fun anAccountWithDataIsTrackedAsNeedsKey() =
    scope.runTest {
      env.remote.accounts[TestAccount.uid] = RemoteAccount(ByteArray(0), 1)
      vm.onSignIn(Activity())
      assertEquals(
        "sync_sign_in_finished" to mapOf("result" to "needs_key"),
        tracker.recordedActions().last(),
      )
    }

  @Test
  fun closingTheGoogleSheetIsTrackedAsCancelled() =
    scope.runTest {
      env.auth.signInResult = SignInResult.Cancelled
      vm.onSignIn(Activity())
      assertEquals(
        "sync_sign_in_finished" to mapOf("result" to "cancelled"),
        tracker.recordedActions().last(),
      )
    }

  @Test
  fun cancellingTheActivationIsTrackedOnce() =
    scope.runTest {
      vm.onSignIn(Activity())
      vm.onCancelActivation()
      vm.onCancelActivation()
      assertEquals(SyncState.SignedOut, env.repository.state.value)
      assertEquals(
        listOf("sync_activation_cancelled" to emptyMap<String, String>()),
        tracker.recordedActions().drop(2),
      )
    }

  @Test
  fun changingAccountWithoutTheWordsIsTrackedAsSignedOutFromNeedsKey() =
    scope.runTest {
      env.remote.accounts[TestAccount.uid] = RemoteAccount(ByteArray(0), 1)
      vm.onSignIn(Activity())
      vm.onSignOut()
      assertEquals(SyncState.SignedOut, env.repository.state.value)
      assertEquals("sync_signed_out" to mapOf("from" to "needs_key"), tracker.recordedActions().last())
    }
}

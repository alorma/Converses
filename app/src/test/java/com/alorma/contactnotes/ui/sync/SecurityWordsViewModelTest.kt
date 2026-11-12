package com.alorma.contactnotes.ui.sync

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeSyncEnvironment
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityWordsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val scope = TestScope(UnconfinedTestDispatcher())
  private val env = FakeSyncEnvironment(scope)
  private val repository = env.repository
  private val store = ViewModelStore()
  private val tracker = RecordingTracker()

  private suspend fun activating() {
    repository.signIn(Activity())
    repository.unlockWords()
  }

  private fun viewModel(activation: Boolean): SecurityWordsViewModel {
    val factory =
      viewModelFactory {
        initializer { SecurityWordsViewModel(activation, repository, tracker.eventTracker, cleanupScope = scope) }
      }
    return ViewModelProvider.create(store, factory)[SecurityWordsViewModel::class]
  }

  @Test
  fun withoutUnlockedWordsTheScreenClosesItself() =
    scope.runTest {
      repository.signIn(Activity())
      val vm = viewModel(activation = true)
      assertTrue(
        vm.state.value.words
          .isEmpty(),
      )
      assertEquals(SecurityWordsEvent.Close, vm.events.first())
      assertEquals(SyncState.SignedOut, repository.state.value)
    }

  @Test
  fun showsTheTwelveWords() =
    scope.runTest {
      activating()
      assertEquals(
        12,
        viewModel(activation = true)
          .state.value.words.size,
      )
    }

  @Test
  fun continueNeedsTheCheckbox() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.onContinue()
      assertTrue(env.remote.accounts.isEmpty())
      assertTrue(repository.state.value is SyncState.Activating)
    }

  @Test
  fun continueUploadsAndCloses() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.onSavedChange(true)
      vm.onContinue()
      assertEquals(SecurityWordsEvent.Close, vm.events.first())
      assertEquals(SyncState.Ready(TestAccount), repository.state.value)
      assertTrue(TestAccount.uid in env.remote.accounts)
    }

  @Test
  fun offlineStaysOpenAndAllowsRetry() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.onSavedChange(true)
      env.remote.failure = FakeAccountRemote.Failure.Offline
      vm.onContinue()
      assertEquals(SecurityWordsEvent.Offline, vm.events.first())
      assertFalse(vm.state.value.working)
      assertTrue(repository.state.value is SyncState.Activating)
      env.remote.failure = null
      vm.onContinue()
      assertEquals(SecurityWordsEvent.Close, vm.events.first())
    }

  @Test
  fun anotherDeviceActivatedMeanwhileClosesAndNeedsTheKey() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.onSavedChange(true)
      env.remote.accounts[TestAccount.uid] =
        com.alorma.contactnotes.data.sync
          .RemoteAccount(ByteArray(0), 1)
      vm.onContinue()
      assertEquals(SecurityWordsEvent.Close, vm.events.first())
      assertEquals(SyncState.NeedsKey(TestAccount), repository.state.value)
    }

  @Test
  fun closingBeforeContinuingSignsOutAndUploadsNothing() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.close()
      assertEquals(SecurityWordsEvent.Close, vm.events.first())
      assertEquals(SyncState.SignedOut, repository.state.value)
      assertTrue(env.remote.accounts.isEmpty())
      assertNull(env.keyStore.stored)
      assertEquals(1, env.auth.signOuts)
    }

  @Test
  fun reviewModeUpdatesTheReviewDate() =
    scope.runTest {
      activating()
      val setup = viewModel(activation = true)
      setup.onSavedChange(true)
      setup.onContinue()
      store.clear()
      env.now = 5_000L
      repository.unlockWords()
      val vm = viewModel(activation = false)
      assertEquals(12, vm.state.value.words.size)
      assertEquals(5_000L, env.local.wordsReviewedAt.value)
    }

  @Test
  fun wordsAreLockedWhenTheViewModelIsCleared() =
    scope.runTest {
      activating()
      viewModel(activation = true)
      assertEquals(12, repository.securityWords()?.size)
      store.clear()
      assertNull(repository.securityWords())
    }

  @Test
  fun tracksCopyAndTheActivationResult() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.onCopied()
      vm.onSavedChange(true)
      env.remote.failure = FakeAccountRemote.Failure.Offline
      vm.onContinue()
      vm.events.first()
      env.remote.failure = null
      vm.onContinue()
      vm.events.first()
      // Tancar després de continuar ja no és cancel·lar
      store.clear()
      assertEquals(
        listOf(
          "sync_words_copied" to emptyMap(),
          "sync_activation_finished" to mapOf("result" to "offline"),
          "sync_activation_finished" to mapOf("result" to "done"),
        ),
        tracker.recordedActions(),
      )
    }

  @Test
  fun closingBeforeContinuingTracksOneCancellation() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.close()
      vm.events.first()
      store.clear()
      assertEquals(listOf("sync_activation_cancelled" to emptyMap<String, String>()), tracker.recordedActions())
    }

  @Test
  fun anotherDeviceActivatedMeanwhileIsTrackedAsAlreadyActive() =
    scope.runTest {
      activating()
      val vm = viewModel(activation = true)
      vm.onSavedChange(true)
      env.remote.accounts[TestAccount.uid] =
        com.alorma.contactnotes.data.sync
          .RemoteAccount(ByteArray(0), 1)
      vm.onContinue()
      vm.events.first()
      assertEquals(
        listOf("sync_activation_finished" to mapOf("result" to "already_active")),
        tracker.recordedActions(),
      )
    }

  @Test
  fun clearingTheViewModelInActivationSignsOut() =
    scope.runTest {
      activating()
      viewModel(activation = true)
      store.clear()
      assertEquals(SyncState.SignedOut, repository.state.value)
      assertTrue(env.remote.accounts.isEmpty())
    }
}

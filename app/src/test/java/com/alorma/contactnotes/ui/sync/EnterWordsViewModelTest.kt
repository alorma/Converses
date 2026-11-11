package com.alorma.contactnotes.ui.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.RemoteAccount
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeSyncEnvironment
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class EnterWordsViewModelTest {
  @get:Rule val mainRule = MainDispatcherRule()

  private val scope = TestScope(UnconfinedTestDispatcher())
  private val env = FakeSyncEnvironment(scope)
  private val secret = RecoverySecret.generate()
  private val goodWords = secret.words

  /** Un compte amb dades al núvol i aquest dispositiu sense clau: l'estat és NeedsKey. */
  private fun viewModel(): EnterWordsViewModel {
    env.remote.accounts[TestAccount.uid] =
      RemoteAccount(KeyWrap.wrap(DataKey.generate(), secret, TestAccount.uid), SYNC_FORMAT)
    runBlocking { env.repository.signIn(Activity()) }
    assertEquals(SyncState.NeedsKey(TestAccount), env.repository.state.value)
    return EnterWordsViewModel(env.repository)
  }

  private fun EnterWordsViewModel.fill(words: List<String>) {
    onPaste(words.joinToString(" "))
  }

  @Test
  fun pastingTwelveWordsFillsAllTheFieldsWhateverTheFormat() {
    val vm = viewModel()
    val messy = goodWords.mapIndexed { i, w -> "${i + 1}. ${w.uppercase()}" }.joinToString("\n")
    assertNull(vm.onPaste(messy))
    assertEquals(goodWords, vm.state.value.words)
    assertTrue(vm.state.value.canSubmit)
  }

  @Test
  fun pastingFewerWordsFillsFromTheFocusedField() {
    val vm = viewModel()
    vm.onFocus(4)
    assertEquals(0, vm.onPaste("abandon ability"))
    assertEquals(
      listOf("", "", "", "", "abandon", "ability"),
      vm.state.value.words
        .take(6),
    )
  }

  @Test
  fun aSpaceFinishesTheWordAndMovesOn() {
    val vm = viewModel()
    assertNull(vm.onWordChange(0, "Aban"))
    assertEquals("aban", vm.state.value.words[0])
    assertEquals(1, vm.onWordChange(0, "abandon "))
    assertEquals("abandon", vm.state.value.words[0])
    assertNull(vm.onWordChange(11, "zoo "))
  }

  @Test
  fun severalWordsTypedIntoOneFieldAreSpread() {
    val vm = viewModel()
    // El focus va al primer camp buit que queda
    assertEquals(0, vm.onWordChange(1, "able about above"))
    assertEquals(
      listOf("", "able", "about", "above"),
      vm.state.value.words
        .take(4),
    )
  }

  @Test
  fun suggestionsFollowTheFocusedFieldAndTapFillsIt() {
    val vm = viewModel()
    vm.onFocus(0)
    vm.onWordChange(0, "a")
    assertTrue(
      vm.state.value.suggestions
        .isEmpty(),
    )
    vm.onWordChange(0, "act")
    assertEquals(listOf("act", "action", "actor", "actress"), vm.state.value.suggestions)
    assertEquals(1, vm.onSuggestion(0, "action"))
    assertEquals("action", vm.state.value.words[0])
    vm.onWordChange(1, "zoo")
    vm.onFocus(1)
    assertTrue(
      vm.state.value.suggestions
        .isEmpty(),
    )
  }

  @Test
  fun aCompleteUnknownWordIsMarkedOnceItLosesTheFocus() {
    val vm = viewModel()
    vm.onFocus(2)
    vm.onWordChange(2, "zzzz")
    assertFalse(vm.state.value.isUnknown(2))
    vm.onBlur(2)
    assertTrue(vm.state.value.isUnknown(2))
    assertFalse(vm.state.value.canSubmit)
  }

  @Test
  fun acceptedWordsOpenTheAccountAndCloseTheScreen() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(goodWords)
      assertTrue(vm.open.value)
      vm.submit()
      assertEquals(SyncState.Ready(TestAccount), env.repository.state.value)
      assertFalse(vm.open.value)
      assertFalse(vm.state.value.submitting)
    }

  @Test
  fun wrongWordsShowTheNoteAndKeepAskingForTheKey() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(RecoverySecret.generate().words)
      vm.submit()
      assertTrue(vm.state.value.wrongWords)
      assertEquals(SyncState.NeedsKey(TestAccount), env.repository.state.value)
      assertTrue(vm.open.value)
    }

  @Test
  fun twelveValidWordsInTheWrongOrderAreTheSameProblem() =
    scope.runTest {
      val vm = viewModel()
      // La suma de verificació ho detecta amb gairebé qualsevol permutació: es busca una que falli
      val shuffled =
        (1 until 12).map { k -> goodWords.drop(k) + goodWords.take(k) }.first {
          Bip39.decode(it) is com.alorma.contactnotes.data.sync.crypto.Bip39Result.BadChecksum
        }
      vm.fill(shuffled)
      vm.submit()
      assertTrue(vm.state.value.wrongWords)
      assertEquals(SyncState.NeedsKey(TestAccount), env.repository.state.value)
    }

  @Test
  fun offlineShowsAMessageAndCanBeRetried() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(goodWords)
      env.remote.failure = FakeAccountRemote.Failure.Offline
      vm.submit()
      assertEquals(EnterWordsMessage.Offline, vm.messages.first())
      assertTrue(vm.state.value.canSubmit)
      env.remote.failure = null
      vm.submit()
      assertEquals(SyncState.Ready(TestAccount), env.repository.state.value)
    }

  @Test
  fun anAccountWithoutDataSignsOutAndClosesTheScreen() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(goodWords)
      env.remote.accounts.clear()
      vm.submit()
      assertEquals(EnterWordsMessage.Failed, vm.messages.first())
      assertFalse(vm.open.value)
    }

  @Test
  fun editingAWordClearsTheNote() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(RecoverySecret.generate().words)
      vm.submit()
      assertTrue(vm.state.value.wrongWords)
      vm.onWordChange(3, "abandon")
      assertFalse(vm.state.value.wrongWords)
    }

  @Test
  fun theStateNeverPrintsTheWords() {
    val vm = viewModel()
    vm.fill(goodWords)
    assertEquals("EnterWordsUiState", vm.state.value.toString())
  }
}

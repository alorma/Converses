package com.alorma.contactnotes.ui.sync

import android.app.Activity
import com.alorma.contactnotes.data.sync.RemoteAccount
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.Bip39Result
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.KeyWrap
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT
import com.alorma.contactnotes.testing.FakeAccountRemote
import com.alorma.contactnotes.testing.FakeSyncEnvironment
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import com.alorma.contactnotes.testing.TestAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  private val tracker = RecordingTracker()

  /** Un compte amb dades al núvol i aquest dispositiu sense clau: l'estat és NeedsKey. */
  private fun viewModel(): EnterWordsViewModel {
    env.remote.accounts[TestAccount.uid] =
      RemoteAccount(KeyWrap.wrap(DataKey.generate(), secret, TestAccount.uid), SYNC_FORMAT)
    runBlocking { env.repository.signIn(Activity()) }
    assertEquals(SyncState.NeedsKey(TestAccount), env.repository.state.value)
    return EnterWordsViewModel(env.repository, tracker.eventTracker)
  }

  private fun EnterWordsViewModel.fill(words: List<String>) = onTextChange(words.joinToString(" "))

  @Test
  fun pastingTwelveWordsWhateverTheFormatCountsAndCanBeSubmitted() {
    val vm = viewModel()
    val messy = goodWords.mapIndexed { i, w -> "${i + 1}. ${w.uppercase()}" }.joinToString("\n")
    vm.onPaste(messy)
    assertEquals(goodWords, vm.state.value.words)
    assertEquals(12, vm.state.value.wordCount)
    assertTrue(vm.state.value.canSubmit)
    // El text és el que s'ha enganxat: no es reformata
    assertEquals(messy, vm.state.value.text)
  }

  @Test
  fun theCounterFollowsTheWordsTyped() {
    val vm = viewModel()
    assertEquals(0, vm.state.value.wordCount)
    vm.onTextChange("able about, ab")
    assertEquals(3, vm.state.value.wordCount)
    assertFalse(vm.state.value.canSubmit)
  }

  @Test
  fun suggestionsFollowTheLastWordAndTapCompletesIt() {
    val vm = viewModel()
    vm.onTextChange("able a")
    assertTrue(
      vm.state.value.suggestions
        .isEmpty(),
    )
    vm.onTextChange("able act")
    assertEquals(listOf("act", "action", "actor", "actress"), vm.state.value.suggestions)
    vm.onSuggestion("action")
    assertEquals("able action ", vm.state.value.text)
    // Acabat en espai ja no s'està escrivint cap paraula
    assertTrue(
      vm.state.value.suggestions
        .isEmpty(),
    )
  }

  @Test
  fun noSuggestionsWhenTheWordIsTheOnlyMatch() {
    val vm = viewModel()
    vm.onTextChange("zoo")
    assertTrue(
      vm.state.value.suggestions
        .isEmpty(),
    )
  }

  @Test
  fun anUnknownWordIsMarkedOnlyOnceItIsComplete() {
    val vm = viewModel()
    vm.onFocusChange(true)
    vm.onTextChange("able zzzz")
    assertTrue(
      vm.state.value.unknownRanges
        .isEmpty(),
    )
    vm.onTextChange("able zzzz ")
    assertEquals(listOf(5..8), vm.state.value.unknownRanges)
    assertFalse(vm.state.value.canSubmit)
    // Sense focus, l'última paraula també es dona per completa
    vm.onTextChange("able zzzz")
    vm.onFocusChange(false)
    assertEquals(listOf(5..8), vm.state.value.unknownRanges)
  }

  @Test
  fun theRangesMatchTheWordsOfBip39() {
    val text = "1. Able, ZZZZ\nabout"
    assertEquals(Bip39.split(text).size, Regex("[A-Za-z]+").findAll(text).count())
    assertEquals(listOf(9..12), unknownRanges(text, emptySet(), lastInProgress = false))
  }

  @Test
  fun elevenOrThirteenWordsCannotBeSubmitted() {
    val vm = viewModel()
    vm.fill(goodWords.take(11))
    assertFalse(vm.state.value.canSubmit)
    vm.fill(goodWords + "abandon")
    assertFalse(vm.state.value.canSubmit)
    vm.submit()
    assertFalse(vm.state.value.submitting)
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
  fun eachSubmissionTracksOnlyItsResult() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(RecoverySecret.generate().words)
      vm.submit()
      vm.fill(goodWords)
      vm.submit()
      assertEquals(
        listOf(
          "sync_words_submitted" to mapOf("result" to "wrong"),
          "sync_words_submitted" to mapOf("result" to "accepted"),
        ),
        tracker.recordedActions(),
      )
    }

  @Test
  fun twelveValidWordsInTheWrongOrderAreTheSameProblem() =
    scope.runTest {
      val vm = viewModel()
      // La suma de verificació ho detecta amb gairebé qualsevol permutació: es busca una que falli
      val shuffled =
        (1 until 12).map { k -> goodWords.drop(k) + goodWords.take(k) }.first {
          Bip39.decode(it) is Bip39Result.BadChecksum
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
  fun editingTheTextClearsTheNote() =
    scope.runTest {
      val vm = viewModel()
      vm.fill(RecoverySecret.generate().words)
      vm.submit()
      assertTrue(vm.state.value.wrongWords)
      vm.onTextChange(vm.state.value.text + " ")
      assertFalse(vm.state.value.wrongWords)
    }

  @Test
  fun theStateNeverPrintsTheWords() {
    val vm = viewModel()
    vm.fill(goodWords)
    assertEquals("EnterWordsUiState", vm.state.value.toString())
  }
}

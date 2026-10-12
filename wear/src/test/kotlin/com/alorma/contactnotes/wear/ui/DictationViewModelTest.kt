package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.wear.data.SpeechError
import com.alorma.contactnotes.wear.data.SpeechEvent
import com.alorma.contactnotes.wear.data.SpeechSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictationViewModelTest {
  private class FakeSpeech : SpeechSource {
    var channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    var listens = 0
    var languages = mutableListOf<String>()
    var finished = 0

    override fun listen(languageTag: String): Flow<SpeechEvent> {
      listens++
      languages += languageTag
      return channel.receiveAsFlow()
    }

    override fun finish() {
      finished++
    }
  }

  private val speech = FakeSpeech()

  @Before
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After
  fun tearDown() = Dispatchers.resetMain()

  private fun viewModel() = DictationViewModel(speech, "ca-ES")

  @Test
  fun `does not open the microphone until it is started`() {
    viewModel()
    assertEquals(0, speech.listens)
  }

  @Test
  fun `starts listening with the app language`() {
    viewModel().start()
    assertEquals(listOf("ca-ES"), speech.languages)
  }

  @Test
  fun `starting twice listens once`() {
    val vm = viewModel()
    vm.start()
    vm.start()
    assertEquals(1, speech.listens)
  }

  @Test
  fun `shows the partial text and the level live`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Partial("Li va"))
    speech.channel.trySend(SpeechEvent.Level(0.5f))
    speech.channel.trySend(SpeechEvent.Partial("Li va agradar"))
    assertEquals(DictationUiState.Listening("Li va agradar", 0.5f), vm.state.value)
  }

  @Test
  fun `the final text ends the dictation`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Partial("Li va"))
    speech.channel.trySend(SpeechEvent.Final("Li va agradar el llibre"))
    assertEquals(DictationUiState.Done("Li va agradar el llibre"), vm.state.value)
  }

  @Test
  fun `a blank final text is a failure`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Final("  "))
    assertEquals(DictationUiState.Failed(SpeechError.NoSpeech), vm.state.value)
  }

  @Test
  fun `a recognizer error is shown`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Error(SpeechError.Microphone))
    assertEquals(DictationUiState.Failed(SpeechError.Microphone), vm.state.value)
  }

  @Test
  fun `events after the end do not change the result`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Final("Hola"))
    speech.channel.trySend(SpeechEvent.Partial("tard"))
    speech.channel.trySend(SpeechEvent.Level(1f))
    assertEquals(DictationUiState.Done("Hola"), vm.state.value)
  }

  @Test
  fun `finish asks the recognizer to close and waits for the final text`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Partial("Hola"))
    vm.finish()
    assertEquals(1, speech.finished)
    assertTrue(vm.state.value is DictationUiState.Listening)
    speech.channel.trySend(SpeechEvent.Final("Hola món"))
    assertEquals(DictationUiState.Done("Hola món"), vm.state.value)
  }

  @Test
  fun `finish without a started dictation does nothing to the recognizer`() {
    val vm = viewModel()
    vm.finish()
    assertEquals(0, speech.finished)
    assertEquals(DictationUiState.Failed(SpeechError.NoSpeech), vm.state.value)
  }

  @Test
  fun `retry listens again after a failure`() {
    val vm = viewModel()
    vm.start()
    speech.channel.trySend(SpeechEvent.Error(SpeechError.NoSpeech))
    speech.channel = Channel(Channel.UNLIMITED)
    vm.retry()
    assertEquals(2, speech.listens)
    assertFalse(vm.state.value is DictationUiState.Failed)
    speech.channel.trySend(SpeechEvent.Final("Ara sí"))
    assertEquals(DictationUiState.Done("Ara sí"), vm.state.value)
  }
}

package com.alorma.contactnotes.wear.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Per què el dictat no ha pogut seguir (P4). */
enum class SpeechError {
  /** El rellotge no té cap reconeixedor de veu. */
  Unavailable,

  /** No s'ha sentit res. */
  NoSpeech,

  /** Sense micròfon o permís revocat. */
  Microphone,

  /** Qualsevol altre error del reconeixedor (xarxa, ocupat…). */
  Failed,
}

sealed interface SpeechEvent {
  /** Text parcial: substitueix l'anterior. */
  data class Partial(
    val text: String,
  ) : SpeechEvent

  /** Text definitiu: el flux s'acaba just després. */
  data class Final(
    val text: String,
  ) : SpeechEvent

  /** Nivell del micròfon entre 0 i 1, per animar les ones. */
  data class Level(
    val value: Float,
  ) : SpeechEvent

  data class Error(
    val reason: SpeechError,
  ) : SpeechEvent
}

/** Un dictat. És una interfície perquè el ViewModel es pugui provar sense el reconeixedor del sistema. */
interface SpeechSource {
  /** Escolta fins que hi ha un [SpeechEvent.Final] o un [SpeechEvent.Error]; cancel·lar el flux atura el micròfon. */
  fun listen(languageTag: String): Flow<SpeechEvent>

  /** Demana al reconeixedor que tanqui l'escolta i entregui el que té com a [SpeechEvent.Final]. */
  fun finish()
}

/** Rang de `onRmsChanged` que es considera de silenci a veu alta, en dB. */
private const val RMS_MIN = 0f
private const val RMS_MAX = 10f

class AndroidSpeechSource(
  context: Context,
) : SpeechSource {
  private val appContext = context.applicationContext
  private var recognizer: SpeechRecognizer? = null

  override fun listen(languageTag: String): Flow<SpeechEvent> =
    callbackFlow {
      if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
        trySend(SpeechEvent.Error(SpeechError.Unavailable))
        close()
        return@callbackFlow
      }
      val speech = SpeechRecognizer.createSpeechRecognizer(appContext)
      recognizer = speech
      var last = ""
      speech.setRecognitionListener(
        object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) = Unit

          override fun onBeginningOfSpeech() = Unit

          override fun onRmsChanged(rmsdB: Float) {
            trySend(SpeechEvent.Level(((rmsdB - RMS_MIN) / (RMS_MAX - RMS_MIN)).coerceIn(0f, 1f)))
          }

          override fun onBufferReceived(buffer: ByteArray?) = Unit

          override fun onEndOfSpeech() = Unit

          override fun onError(error: Int) {
            // Si ja s'havia sentit alguna cosa, es conserva en lloc de perdre-ho
            if (last.isNotBlank() && error == SpeechRecognizer.ERROR_NO_MATCH) {
              trySend(SpeechEvent.Final(last))
            } else {
              trySend(SpeechEvent.Error(error.toSpeechError()))
            }
            close()
          }

          override fun onResults(results: Bundle?) {
            trySend(SpeechEvent.Final(results.firstText().ifBlank { last }))
            close()
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults.firstText()
            if (text.isNotBlank()) {
              last = text
              trySend(SpeechEvent.Partial(text))
            }
          }

          override fun onEvent(
            eventType: Int,
            params: Bundle?,
          ) = Unit
        },
      )
      speech.startListening(
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag.ifBlank { Locale.getDefault().toLanguageTag() })
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        },
      )
      awaitClose {
        speech.destroy()
        if (recognizer === speech) recognizer = null
      }
    }

  override fun finish() {
    recognizer?.stopListening()
  }
}

private fun Bundle?.firstText(): String =
  this
    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    ?.firstOrNull()
    .orEmpty()

private fun Int.toSpeechError(): SpeechError =
  when (this) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NoSpeech
    SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.Microphone
    else -> SpeechError.Failed
  }

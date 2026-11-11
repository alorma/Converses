package com.alorma.contactnotes.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.data.sync.WordsOutcome
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.Bip39Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Els suggeriments surten a partir d'aquesta mida: amb una sola lletra n'hi hauria massa per ser útils. */
private const val SUGGESTION_MIN_PREFIX = 2
private const val SUGGESTION_LIMIT = 4

/** Les mateixes paraules que parteix [Bip39.split]: seqüències de lletres ASCII. Així el color cau on toca. */
private val wordRegex = Regex("[A-Za-z]+")

/**
 * Els trams de [text] que són paraules, amb la posició que ocupen (0, 1, 2…) i sense tocar el que l'usuari ha escrit.
 */
private fun wordMatches(text: String): List<MatchResult> = wordRegex.findAll(text).toList()

/** L'última paraula s'està escrivint encara: el text acaba en una lletra. */
private fun endsInWord(text: String): Boolean = text.lastOrNull()?.let { it in 'A'..'Z' || it in 'a'..'z' } == true

/**
 * Els trams de [text] que cal pintar d'error: les paraules que no són de la llista i les [rejected] (per posició).
 * Si [lastInProgress], l'última paraula encara s'està escrivint i no compta, tret que sigui de [rejected].
 */
fun unknownRanges(
  text: String,
  rejected: Set<Int>,
  lastInProgress: Boolean,
): List<IntRange> {
  val matches = wordMatches(text)
  return matches
    .withIndex()
    .filter { (index, match) ->
      val inProgress = lastInProgress && index == matches.lastIndex && endsInWord(text)
      index in rejected || (!inProgress && !Bip39.isWord(match.value))
    }.map { it.value.range }
}

/**
 * El text de les paraules tal com l'usuari l'ha escrit o enganxat. [focused] diu si el camp té el focus; [rejected]
 * són les posicions de paraula que el repositori ha dit que no són de la llista. Mai es mostra en un log: [toString]
 * no porta el text.
 */
data class EnterWordsUiState(
  val text: String = "",
  val focused: Boolean = false,
  val rejected: Set<Int> = emptySet(),
  /** «Aquestes paraules no obren les dades d'aquest compte». */
  val wrongWords: Boolean = false,
  val submitting: Boolean = false,
) {
  val words: List<String> get() = Bip39.split(text)

  val wordCount: Int get() = wordMatches(text).size

  /** Si el camp té el focus i el text acaba en una lletra, l'última paraula encara no és completa. */
  private val lastInProgress: Boolean get() = focused

  /** Els trams que es pinten d'error (vegeu [unknownRanges]). */
  val unknownRanges: List<IntRange> get() = unknownRanges(text, rejected, lastInProgress)

  val hasUnknown: Boolean get() = unknownRanges.isNotEmpty()

  /** Les paraules de la llista que comencen com l'última que s'escriu; res si ja és l'única opció. */
  val suggestions: List<String>
    get() {
      if (!endsInWord(text)) return emptyList()
      val word = wordMatches(text).last().value.lowercase()
      if (word.length < SUGGESTION_MIN_PREFIX) return emptyList()
      return Bip39.suggestions(word, SUGGESTION_LIMIT).takeUnless { it == listOf(word) }.orEmpty()
    }

  val canSubmit: Boolean get() = !submitting && words.size == Bip39.WORD_COUNT && words.all(Bip39::isWord)

  override fun toString(): String = "EnterWordsUiState"
}

/** El que la pantalla ha de dir una sola vegada, amb un snackbar. */
enum class EnterWordsMessage { Offline, Failed }

/**
 * Escriure les 12 paraules en un dispositiu nou. Les paraules només viuen aquí, en memòria: no passen per cap estat
 * desat (`rememberSaveable`, `SavedStateHandle`) perquè no s'escriguin al disc.
 */
class EnterWordsViewModel(
  private val repository: SyncRepository,
) : ViewModel() {
  private val mutableState = MutableStateFlow(EnterWordsUiState())
  val state: StateFlow<EnterWordsUiState> = mutableState.asStateFlow()

  /** Deixa de ser `true` quan el compte ja no espera la clau: acceptada, o tancada la sessió. Llavors es tanca. */
  val open: StateFlow<Boolean> =
    repository.state
      .map { it is SyncState.NeedsKey }
      .stateIn(viewModelScope, SharingStarted.Eagerly, repository.state.value is SyncState.NeedsKey)

  private val messageChannel = Channel<EnterWordsMessage>(Channel.BUFFERED)
  val messages: Flow<EnterWordsMessage> = messageChannel.receiveAsFlow()

  fun onFocusChange(focused: Boolean) = mutableState.update { it.copy(focused = focused) }

  /** El text ha canviat (escrit o enganxat amb el teclat). Qualsevol canvi esborra la nota i les marques del servidor. */
  fun onTextChange(text: String) {
    mutableState.update { if (it.text == text) it else it.copy(text = text, rejected = emptySet(), wrongWords = false) }
  }

  /** «Enganxa»: el que hi hagi al porta-retalls substitueix el text. */
  fun onPaste(text: String) = onTextChange(text)

  /** Tocar un suggeriment: completa l'última paraula i hi afegeix un espai. */
  fun onSuggestion(word: String) {
    val text = mutableState.value.text
    val last = wordMatches(text).lastOrNull()?.takeIf { endsInWord(text) } ?: return
    onTextChange(text.substring(0, last.range.first) + word + " ")
  }

  fun submit() {
    val current = mutableState.value
    if (!current.canSubmit) return
    mutableState.update { it.copy(submitting = true) }
    viewModelScope.launch {
      val outcome = repository.submitWords(current.words)
      mutableState.update { it.copy(submitting = false) }
      when (outcome) {
        // Acceptades: el compte passa a Ready i `open` tanca la pantalla. Sense clau (compte buit) passa el mateix.
        WordsOutcome.Accepted -> Unit
        WordsOutcome.Wrong -> mutableState.update { it.copy(wrongWords = true) }
        is WordsOutcome.Invalid -> invalid(outcome.problem)
        WordsOutcome.Offline -> messageChannel.send(EnterWordsMessage.Offline)
        WordsOutcome.Failed -> messageChannel.send(EnterWordsMessage.Failed)
      }
    }
  }

  /** Dotze paraules de la llista en un ordre que no val són, per a qui les escriu, el mateix problema que unes altres. */
  private fun invalid(problem: Bip39Result) {
    mutableState.update {
      when (problem) {
        is Bip39Result.UnknownWords -> it.copy(rejected = problem.positions.toSet())
        else -> it.copy(wrongWords = true)
      }
    }
  }
}

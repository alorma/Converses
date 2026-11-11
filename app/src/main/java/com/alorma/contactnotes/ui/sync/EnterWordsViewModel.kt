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

/**
 * Les 12 paraules escrites. [focused] és el camp amb el focus (`null` si cap). [rejected] són les posicions que el
 * repositori ha dit que no són de la llista. Mai es mostra en un log: [toString] no porta les paraules.
 */
data class EnterWordsUiState(
  val words: List<String> = List(Bip39.WORD_COUNT) { "" },
  val focused: Int? = null,
  val rejected: Set<Int> = emptySet(),
  /** «Aquestes paraules no obren les dades d'aquest compte». */
  val wrongWords: Boolean = false,
  val submitting: Boolean = false,
) {
  /** Una paraula completa que no és de la llista. El camp que s'està escrivint encara no compta. */
  fun isUnknown(index: Int): Boolean =
    index in rejected || (index != focused && words[index].isNotEmpty() && !Bip39.isWord(words[index]))

  /** Les paraules de la llista que comencen com el camp actiu; res si ja és l'única opció. */
  val suggestions: List<String>
    get() {
      val word = focused?.let(words::get).orEmpty()
      if (word.length < SUGGESTION_MIN_PREFIX) return emptyList()
      return Bip39.suggestions(word, SUGGESTION_LIMIT).takeUnless { it == listOf(word) }.orEmpty()
    }

  val canSubmit: Boolean get() = !submitting && words.all(Bip39::isWord)

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

  fun onFocus(index: Int) = mutableState.update { it.copy(focused = index) }

  fun onBlur(index: Int) = mutableState.update { if (it.focused == index) it.copy(focused = null) else it }

  /**
   * El camp [index] ha canviat a [raw]. Retorna el camp al qual cal passar el focus: un espai (o un salt de línia)
   * dona la paraula per acabada. Si arriben diverses paraules juntes (enganxades des del teclat), es reparteixen.
   */
  fun onWordChange(
    index: Int,
    raw: String,
  ): Int? {
    val tokens = Bip39.split(raw)
    if (tokens.size > 1) return fill(tokens, from = index)
    val word = tokens.firstOrNull().orEmpty()
    setWord(index, word)
    return if (word.isNotEmpty() && raw.last().isWhitespace()) nextAfter(index) else null
  }

  /** Tocar un suggeriment: omple el camp i passa al següent. */
  fun onSuggestion(
    index: Int,
    word: String,
  ): Int? {
    setWord(index, word)
    return nextAfter(index)
  }

  /**
   * «Enganxa». Amb 12 paraules les posa totes; amb menys, a partir del camp actiu (o del primer buit). Retorna el
   * primer camp buit que queda, si n'hi ha.
   */
  fun onPaste(text: String): Int? {
    val tokens = Bip39.split(text)
    if (tokens.isEmpty()) return null
    val current = mutableState.value
    val from =
      when {
        tokens.size >= Bip39.WORD_COUNT -> 0
        else -> current.focused ?: current.words.indexOfFirst { it.isEmpty() }.coerceAtLeast(0)
      }
    return fill(tokens, from)
  }

  private fun fill(
    tokens: List<String>,
    from: Int,
  ): Int? {
    mutableState.update { current ->
      val words = current.words.toMutableList()
      tokens.forEachIndexed { offset, token -> if (from + offset < words.size) words[from + offset] = token }
      val touched = from until minOf(from + tokens.size, words.size)
      current.copy(words = words, rejected = current.rejected - touched.toSet(), wrongWords = false)
    }
    return mutableState.value.words
      .indexOfFirst { it.isEmpty() }
      .takeIf { it >= 0 }
  }

  private fun setWord(
    index: Int,
    word: String,
  ) {
    mutableState.update { current ->
      current.copy(
        words = current.words.toMutableList().also { it[index] = word },
        rejected = current.rejected - index,
        wrongWords = false,
      )
    }
  }

  private fun nextAfter(index: Int): Int? = (index + 1).takeIf { it < Bip39.WORD_COUNT }

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

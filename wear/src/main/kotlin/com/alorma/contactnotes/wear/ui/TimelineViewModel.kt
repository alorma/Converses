package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.WEAR_MAX_ENTRIES_PER_PERSON
import com.alorma.contactnotes.shared.WEAR_MAX_EXTRA_ENTRIES
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.initialsOf
import com.alorma.contactnotes.wear.data.MoreEntriesResult
import com.alorma.contactnotes.wear.data.MoreEntriesSource
import com.alorma.contactnotes.wear.data.StoredSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TimelineUiState {
  data object Loading : TimelineUiState

  /** La persona ja no existeix o és protegida: la pantalla torna enrere sense mostrar res. */
  data object Unavailable : TimelineUiState

  data class Loaded(
    val name: String,
    val initials: String,
    val color: AvatarColor,
    val avatarShape: AvatarShape,
    val entries: List<EntryItem>,
    /** Hi ha una petició de més entrades en curs: la llista mostra l'indicador de càrrega al final (P16). */
    val loadingMore: Boolean = false,
  ) : TimelineUiState
}

/**
 * Les entrades de la còpia més les que es demanen al mòbil sota demanda. Les extra viuen només aquí, en memòria: no
 * s'escriuen enlloc i s'obliden quan el ViewModel mor en sortir de la pantalla. Se'n guarden com a màxim
 * [WEAR_MAX_EXTRA_ENTRIES].
 */
class TimelineViewModel(
  private val personId: Long,
  private val snapshots: Flow<StoredSnapshot?>,
  private val labels: WearLabels,
  private val moreEntries: MoreEntriesSource,
) : ViewModel() {
  private data class Extra(
    val entries: List<WearEntry> = emptyList(),
    val loading: Boolean = false,
    /** No s'hi demana res més: s'ha arribat al final, al límit, o el mòbil no ha respost. */
    val exhausted: Boolean = false,
  )

  private val extra = MutableStateFlow(Extra())

  val state: StateFlow<TimelineUiState> =
    combine(snapshots, extra) { stored, extra ->
      val person = stored?.snapshot?.people?.firstOrNull { it.id == personId }
      if (person == null || person.isProtected) {
        TimelineUiState.Unavailable
      } else {
        TimelineUiState.Loaded(
          name = person.name,
          initials = initialsOf(person.name),
          color = person.avatarColor,
          avatarShape = person.avatarShape,
          entries = person.allEntries(extra).map { it.toItem(person, labels) },
          loadingMore = extra.loading,
        )
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState.Loading)

  /** Demana la pàgina següent. No fa res si ja n'hi ha una en curs, si s'ha arribat al final o al límit. */
  fun loadMore() {
    val current = extra.value
    if (current.loading || current.exhausted) return
    extra.update { it.copy(loading = true) }
    viewModelScope.launch {
      val person =
        snapshots
          .first()
          ?.snapshot
          ?.people
          ?.firstOrNull { it.id == personId }
      val cursor = person?.takeUnless { it.isProtected }?.allEntries(current)?.lastOrNull()
      // Si la còpia en porta menys de les que viatgen, el mòbil no en té més
      if (person == null || cursor == null || person.entries.size < WEAR_MAX_ENTRIES_PER_PERSON) {
        extra.update { it.copy(loading = false, exhausted = true) }
        return@launch
      }
      when (val result = moreEntries.load(personId, cursor.occurredAt, cursor.id)) {
        is MoreEntriesResult.Page ->
          extra.update { latest ->
            val merged = (latest.entries + result.entries).distinctBy { it.id }.take(WEAR_MAX_EXTRA_ENTRIES)
            Extra(
              entries = merged,
              loading = false,
              exhausted = !result.hasMore || result.entries.isEmpty() || merged.size >= WEAR_MAX_EXTRA_ENTRIES,
            )
          }
        MoreEntriesResult.Unavailable -> extra.update { it.copy(loading = false, exhausted = true) }
      }
    }
  }

  private fun WearPerson.allEntries(extra: Extra): List<WearEntry> =
    (entries + extra.entries)
      .distinctBy { it.id }
      .sortedWith(compareByDescending<WearEntry> { it.occurredAt }.thenByDescending { it.id })
}

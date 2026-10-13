package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.wear.data.DiscardedNotes
import com.alorma.contactnotes.wear.data.PhoneConnection
import com.alorma.contactnotes.wear.data.StoredSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PeopleUiState {
  data object Loading : PeopleUiState

  /** El mòbil encara no ha enviat res. */
  data object NoData : PeopleUiState

  /** El mòbil ha sincronitzat, però encara no hi ha cap persona (ni protegida). */
  data object NoPeople : PeopleUiState

  /** `dataAgeMinutes` només hi és quan el mòbil no és accessible: és l'avís «Dades de fa N min». */
  data class Loaded(
    val people: List<PersonItem>,
    val dataAgeMinutes: Int?,
    val discardedNotes: Int = 0,
  ) : PeopleUiState
}

class PeopleViewModel(
  snapshots: Flow<StoredSnapshot?>,
  phone: PhoneConnection,
  labels: WearLabels,
  private val discarded: DiscardedNotes,
  private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
  val state: StateFlow<PeopleUiState> =
    combine(snapshots, phone.connected, discarded.count) { stored, connected, discardedNotes ->
      if (stored == null) {
        PeopleUiState.NoData
      } else if (stored.snapshot.people.isEmpty()) {
        PeopleUiState.NoPeople
      } else {
        val now = clock()
        PeopleUiState.Loaded(
          people = stored.snapshot.people.map { it.toItem(labels, now) },
          dataAgeMinutes = if (connected) null else ((now - stored.syncedAt) / 60_000L).coerceAtLeast(0).toInt(),
          discardedNotes = discardedNotes,
        )
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState.Loading)

  fun dismissDiscardedNotes() {
    viewModelScope.launch { discarded.clear() }
  }
}

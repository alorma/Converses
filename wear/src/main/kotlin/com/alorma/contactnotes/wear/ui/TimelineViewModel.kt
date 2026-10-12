package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.wear.data.StoredSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface TimelineUiState {
  data object Loading : TimelineUiState

  /** La persona ja no existeix o és protegida: la pantalla torna enrere sense mostrar res. */
  data object Unavailable : TimelineUiState

  data class Loaded(
    val name: String,
    val initials: String,
    val color: AvatarColor,
    val entries: List<EntryItem>,
  ) : TimelineUiState
}

class TimelineViewModel(
  personId: Long,
  snapshots: Flow<StoredSnapshot?>,
  labels: WearLabels,
) : ViewModel() {
  val state: StateFlow<TimelineUiState> =
    snapshots
      .map { stored ->
        val person = stored?.snapshot?.people?.firstOrNull { it.id == personId }
        if (person == null || person.isProtected) {
          TimelineUiState.Unavailable
        } else {
          TimelineUiState.Loaded(
            name = person.name,
            initials = initialsOf(person.name),
            color = person.avatarColor,
            entries = person.entries.map { it.toItem(person, labels) },
          )
        }
      }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState.Loading)
}

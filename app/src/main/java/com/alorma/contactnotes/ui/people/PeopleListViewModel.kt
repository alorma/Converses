package com.alorma.contactnotes.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.PersonListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Sense cap canal marcat es mostren totes les persones; amb canals marcats, les que en tenen algun (OR). */
fun List<PersonListItem>.filterBy(types: Set<ChannelType>): List<PersonListItem> {
  if (types.isEmpty()) return this
  return filter { item -> item.channels.any { it.type in types } }
}

data class PeopleListUiState(
  val people: List<PersonListItem> = emptyList(),
  val total: Int = 0,
  val filter: Set<ChannelType> = emptySet(),
  val isLoading: Boolean = true,
)

class PeopleListViewModel(
  private val archived: Boolean,
  repository: ConversesRepository,
) : ViewModel() {
  private val filter = MutableStateFlow(emptySet<ChannelType>())

  val uiState: StateFlow<PeopleListUiState> =
    combine(repository.observePeople(archived = archived), filter) { people, filter ->
      PeopleListUiState(
        people = if (archived) people else people.filterBy(filter),
        total = people.size,
        filter = filter,
        isLoading = false,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleListUiState())

  fun onFilterToggled(type: ChannelType) {
    filter.update { if (type in it) it - type else it + type }
  }
}

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

enum class ChannelFilter(val types: Set<ChannelType>?) {
  All(null),
  Chat(setOf(ChannelType.DISCORD, ChannelType.TELEGRAM, ChannelType.WHATSAPP)),
  Social(setOf(ChannelType.REDDIT, ChannelType.OTHER)),
  InPerson(setOf(ChannelType.IN_PERSON)),
  Phone(setOf(ChannelType.PHONE)),
}

fun List<PersonListItem>.filterBy(filter: ChannelFilter): List<PersonListItem> {
  val types = filter.types ?: return this
  return filter { item -> item.channels.any { it.type in types } }
}

data class PeopleListUiState(
  val people: List<PersonListItem> = emptyList(),
  val total: Int = 0,
  val filter: ChannelFilter = ChannelFilter.All,
  val isLoading: Boolean = true,
)

class PeopleListViewModel(
  repository: ConversesRepository,
) : ViewModel() {
  private val filter = MutableStateFlow(ChannelFilter.All)

  val uiState: StateFlow<PeopleListUiState> =
    combine(repository.observePeople(), filter) { people, filter ->
      PeopleListUiState(
        people = people.filterBy(filter),
        total = people.size,
        filter = filter,
        isLoading = false,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleListUiState())

  fun onFilterSelected(selected: ChannelFilter) {
    filter.value = selected
  }
}

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

enum class ChannelGroup(val types: Set<ChannelType>) {
  Chat(setOf(ChannelType.WHATSAPP, ChannelType.TELEGRAM, ChannelType.SIGNAL, ChannelType.DISCORD, ChannelType.EMAIL)),
  Social(setOf(ChannelType.X, ChannelType.INSTAGRAM, ChannelType.LINKEDIN, ChannelType.REDDIT)),
  InPerson(setOf(ChannelType.IN_PERSON)),
  Phone(setOf(ChannelType.PHONE)),
}

/** Sense cap grup marcat es mostren totes les persones; amb grups marcats, les que en tenen algun canal (OR). */
fun List<PersonListItem>.filterBy(groups: Set<ChannelGroup>): List<PersonListItem> {
  if (groups.isEmpty()) return this
  val types = groups.flatMapTo(mutableSetOf()) { it.types }
  return filter { item -> item.channels.any { it.type in types } }
}

data class PeopleListUiState(
  val people: List<PersonListItem> = emptyList(),
  val total: Int = 0,
  val filter: Set<ChannelGroup> = emptySet(),
  val isLoading: Boolean = true,
)

class PeopleListViewModel(
  private val archived: Boolean,
  repository: ConversesRepository,
) : ViewModel() {
  private val filter = MutableStateFlow(emptySet<ChannelGroup>())

  val uiState: StateFlow<PeopleListUiState> =
    combine(repository.observePeople(archived = archived), filter) { people, filter ->
      PeopleListUiState(
        people = if (archived) people else people.filterBy(filter),
        total = people.size,
        filter = filter,
        isLoading = false,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleListUiState())

  fun onFilterToggled(group: ChannelGroup) {
    filter.update { if (group in it) it - group else it + group }
  }
}

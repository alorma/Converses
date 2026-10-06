package com.alorma.contactnotes.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PersonDetailUiState(
  val person: PersonEntity? = null,
  val channels: List<ChannelEntity> = emptyList(),
  val facts: List<PinnedFactEntity> = emptyList(),
  val entries: List<EntryEntity> = emptyList(),
  val selectedKinds: Set<EntryKind> = EntryKind.entries.toSet(),
  val saidCount: Int = 0,
  val heardCount: Int = 0,
  val totalCount: Int = 0,
  val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModel(
  personId: Long,
  repository: ConversesRepository,
) : ViewModel() {
  private val selectedKinds = MutableStateFlow(EntryKind.entries.toSet())

  private val timeline = selectedKinds.flatMapLatest { kinds ->
    repository.observeTimeline(personId, kinds).map { kinds to it }
  }

  val uiState: StateFlow<PersonDetailUiState> = combine(
    repository.observePerson(personId),
    repository.observeChannels(personId),
    repository.observePinnedFacts(personId),
    repository.observeCounts(personId),
    timeline,
  ) { person, channels, facts, counts, (kinds, entries) ->
    val byKind = counts.associate { it.kind to it.count }
    PersonDetailUiState(
      person = person,
      channels = channels,
      facts = facts,
      entries = entries,
      selectedKinds = kinds,
      saidCount = byKind[EntryKind.SAID] ?: 0,
      heardCount = byKind[EntryKind.HEARD] ?: 0,
      totalCount = byKind.values.sum(),
      isLoading = false,
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

  fun onKindToggled(kind: EntryKind) {
    selectedKinds.value = selectedKinds.value.let { if (kind in it) it - kind else it + kind }
  }
}

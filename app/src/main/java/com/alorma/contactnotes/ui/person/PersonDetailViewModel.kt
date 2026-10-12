package com.alorma.contactnotes.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
  val person: PersonEntity? = null,
  val channels: List<ChannelType> = emptyList(),
  val facts: List<PinnedFactEntity> = emptyList(),
  val entries: List<EntryEntity> = emptyList(),
  val selectedKinds: Set<EntryKind> = EntryKind.entries.toSet(),
  val saidCount: Int = 0,
  val heardCount: Int = 0,
  val happenedCount: Int = 0,
  val isLoading: Boolean = true,
) {
  fun countOf(kind: EntryKind): Int =
    when (kind) {
      EntryKind.SAID -> saidCount
      EntryKind.HEARD -> heardCount
      EntryKind.HAPPENED -> happenedCount
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModel(
  private val personId: Long,
  private val repository: ConversesRepository,
  private val exporter: PersonExporter,
) : ViewModel() {
  private val selectedKinds = MutableStateFlow(EntryKind.entries.toSet())

  private val timeline =
    selectedKinds.flatMapLatest { kinds ->
      repository.observeTimeline(personId, kinds).map { kinds to it }
    }

  val uiState: StateFlow<PersonDetailUiState> =
    combine(
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
        happenedCount = byKind[EntryKind.HAPPENED] ?: 0,
        isLoading = false,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

  /** Persones a les quals es pot moure una entrada. */
  val otherPeople: StateFlow<List<PersonEntity>> =
    repository
      .observePeople()
      .map { items -> items.map { it.person }.filter { it.id != personId } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun onKindToggled(kind: EntryKind) {
    selectedKinds.value = selectedKinds.value.let { if (kind in it) it - kind else it + kind }
  }

  fun addEntry(
    kind: EntryKind,
    channel: ChannelType,
    text: String,
    occurredAt: Long?,
  ) {
    viewModelScope.launch {
      repository.addEntry(
        EntryEntity(
          personId = personId,
          kind = kind,
          channelType = channel,
          text = text,
          occurredAt = occurredAt ?: System.currentTimeMillis(),
        ),
      )
    }
  }

  fun updateEntry(
    entry: EntryEntity,
    kind: EntryKind,
    channel: ChannelType,
    text: String,
    occurredAt: Long?,
  ) {
    viewModelScope.launch {
      repository.updateEntry(
        entry.copy(kind = kind, channelType = channel, text = text, occurredAt = occurredAt ?: entry.occurredAt),
      )
    }
  }

  fun moveEntry(
    entry: EntryEntity,
    toPersonId: Long,
  ) {
    viewModelScope.launch { repository.moveEntry(entry, toPersonId) }
  }

  fun deleteEntry(entry: EntryEntity) {
    viewModelScope.launch { repository.deleteEntry(entry) }
  }

  fun restoreEntry(entry: EntryEntity) {
    viewModelScope.launch { repository.addEntry(entry) }
  }

  suspend fun countDay(entry: EntryEntity): Int {
    val (from, to) = dayRange(entry)
    return repository.countEntriesInRange(personId, entry.channelType, from, to)
  }

  fun deleteDay(entry: EntryEntity) {
    viewModelScope.launch {
      val (from, to) = dayRange(entry)
      repository.deleteEntriesInRange(personId, entry.channelType, from, to)
    }
  }

  fun setArchived(
    archived: Boolean,
    onDone: () -> Unit,
  ) {
    val person = uiState.value.person ?: return
    viewModelScope.launch {
      repository.setArchived(person, archived)
      onDone()
    }
  }

  fun export() {
    viewModelScope.launch {
      val name = uiState.value.person?.name ?: return@launch
      exporter.buildText(personId)?.let { exporter.share(name, it) }
    }
  }

  private fun dayRange(entry: EntryEntity): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(entry.occurredAt).atZone(zone).toLocalDate()
    return day.atStartOfDay(zone).toInstant().toEpochMilli() to
      day
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
  }
}

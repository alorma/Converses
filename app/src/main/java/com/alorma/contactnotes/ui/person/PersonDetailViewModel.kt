package com.alorma.contactnotes.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.ui.entry.EntryChannel
import com.alorma.contactnotes.ui.tracking.Actions
import com.alorma.contactnotes.ui.tracking.EntryMode
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
  /** Noms dels canals propis per id, per mostrar-los a la cronologia en lloc del grup. */
  val customChannelNames: Map<Long, String> = emptyMap(),
  /** Tasques pendents (9d), en l'ordre del DAO: amb data primer, de la més antiga a la més nova. */
  val openTasks: List<TaskEntity> = emptyList(),
  /** Tasques fetes (9f), de la més recent a la més antiga. */
  val doneTasks: List<TaskEntity> = emptyList(),
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
  private val tracker: EventTracker,
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
    }.combine(repository.observeCustomChannels()) { state, channels ->
      state.copy(customChannelNames = channels.associate { it.channel.id to it.channel.name })
    }.combine(
      combine(repository.observeOpenTasks(personId), repository.observeDoneTasks(personId), ::Pair),
    ) { state, (open, done) ->
      state.copy(openTasks = open, doneTasks = done)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

  /** Persones a les quals es pot moure una entrada. */
  val otherPeople: StateFlow<List<PersonEntity>> =
    repository
      .observePeople()
      .map { items -> items.map { it.person }.filter { it.id != personId } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun onKindToggled(kind: EntryKind) {
    selectedKinds.value = selectedKinds.value.let { if (kind in it) it - kind else it + kind }
    tracker.trackAction(Actions.KindFilterToggled(kind))
  }

  /** [mode] diu si el full s'ha obert amb el «Qui» ja triat (entrada fantasma). */
  fun addEntry(
    kind: EntryKind,
    channel: EntryChannel,
    text: String,
    occurredAt: Long?,
    mode: EntryMode = EntryMode.NEW,
  ) {
    tracker.trackAction(Actions.EntrySaved(mode, kind, channel))
    viewModelScope.launch {
      repository.addEntry(
        EntryEntity(
          personId = personId,
          kind = kind,
          channelType = channel.group,
          customChannelId = channel.customChannelId,
          text = text,
          occurredAt = occurredAt ?: System.currentTimeMillis(),
        ),
      )
    }
  }

  fun updateEntry(
    entry: EntryEntity,
    kind: EntryKind,
    channel: EntryChannel,
    text: String,
    occurredAt: Long?,
  ) {
    tracker.trackAction(Actions.EntrySaved(EntryMode.EDIT, kind, channel))
    viewModelScope.launch {
      repository.updateEntry(
        entry.copy(
          kind = kind,
          channelType = channel.group,
          customChannelId = channel.customChannelId,
          text = text,
          occurredAt = occurredAt ?: entry.occurredAt,
        ),
      )
    }
  }

  fun moveEntry(
    entry: EntryEntity,
    toPersonId: Long,
  ) {
    tracker.trackAction(Actions.EntryMoved())
    viewModelScope.launch { repository.moveEntry(entry, toPersonId) }
  }

  fun deleteEntry(entry: EntryEntity) {
    tracker.trackAction(Actions.EntryDeleted())
    viewModelScope.launch { repository.deleteEntry(entry) }
  }

  fun restoreEntry(entry: EntryEntity) {
    tracker.trackAction(Actions.EntryRestored())
    viewModelScope.launch { repository.addEntry(entry) }
  }

  suspend fun countDay(entry: EntryEntity): Int {
    val (from, to) = dayRange(entry)
    return repository.countEntriesInRange(personId, entry.channelType, from, to)
  }

  fun deleteDay(entry: EntryEntity) {
    tracker.trackAction(Actions.DayDeleted())
    viewModelScope.launch {
      val (from, to) = dayRange(entry)
      repository.deleteEntriesInRange(personId, entry.channelType, from, to)
    }
  }

  fun markTaskDone(task: TaskEntity) {
    tracker.trackAction(Actions.TaskDone())
    viewModelScope.launch { repository.markTaskDone(task) }
  }

  /** «Desfés» del snackbar ([fromUndo]) o el check d'una feta. */
  fun reopenTask(
    task: TaskEntity,
    fromUndo: Boolean = false,
  ) {
    tracker.trackAction(Actions.TaskReopened(fromUndo))
    viewModelScope.launch { repository.reopenTask(task) }
  }

  fun setArchived(
    archived: Boolean,
    onDone: () -> Unit,
  ) {
    val person = uiState.value.person ?: return
    tracker.trackAction(Actions.PersonArchived(archived))
    viewModelScope.launch {
      repository.setArchived(person, archived)
      onDone()
    }
  }

  fun export() {
    tracker.trackAction(Actions.PersonExported())
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

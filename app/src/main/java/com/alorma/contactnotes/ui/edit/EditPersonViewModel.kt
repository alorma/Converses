package com.alorma.contactnotes.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import com.alorma.contactnotes.ui.person.PersonExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FactDraft(val key: Long, val label: String, val value: String)

data class ChannelDraft(val key: Long, val type: ChannelType, val handle: String)

data class EditSnapshot(
  val name: String = "",
  val alias: String = "",
  val shape: AvatarShape = AvatarShape.Cookie9Sided,
  val color: AvatarColor = AvatarColor.Lavender,
  val isProtected: Boolean = false,
  val facts: List<FactDraft> = emptyList(),
  val channels: List<ChannelDraft> = emptyList(),
)

data class EditPersonUiState(
  val isNew: Boolean,
  val loaded: Boolean,
  val name: String = "",
  val alias: String = "",
  val shape: AvatarShape = AvatarShape.Cookie9Sided,
  val color: AvatarColor = AvatarColor.Lavender,
  val isProtected: Boolean = false,
  val facts: List<FactDraft> = emptyList(),
  val channels: List<ChannelDraft> = emptyList(),
  val aliasTaken: Boolean = false,
  val entryCount: Int = 0,
  val baseline: EditSnapshot = EditSnapshot(),
) {
  private val snapshot: EditSnapshot
    get() = EditSnapshot(name, alias, shape, color, isProtected, facts, channels)

  val canSave: Boolean get() = loaded && name.isNotBlank() && !aliasTaken && (isNew || snapshot != baseline)
}

class EditPersonViewModel(
  private val personId: Long,
  private val repository: ConversesRepository,
  private val exporter: PersonExporter,
) : ViewModel() {
  private var original: PersonEntity? = null
  private var nextKey = 1L

  private val _state = MutableStateFlow(EditPersonUiState(isNew = personId == 0L, loaded = personId == 0L))
  val state: StateFlow<EditPersonUiState> = _state

  init {
    if (personId != 0L) load()
  }

  private fun load() {
    viewModelScope.launch {
      val person = repository.observePerson(personId).first() ?: return@launch
      val channels = repository.observeChannels(personId).first()
      val facts = repository.observePinnedFacts(personId).first()
      original = person
      val loadedFacts = facts.map { FactDraft(nextKey++, it.label, it.value) }
      val loadedChannels = channels.map { ChannelDraft(nextKey++, it.type, it.handle.orEmpty()) }
      _state.value = EditPersonUiState(
        isNew = false,
        loaded = true,
        name = person.name,
        alias = person.alias.orEmpty(),
        shape = person.avatarShape,
        color = person.avatarColor,
        isProtected = person.isProtected,
        facts = loadedFacts,
        channels = loadedChannels,
        entryCount = repository.getEntries(personId).size,
        baseline = EditSnapshot(
          name = person.name,
          alias = person.alias.orEmpty(),
          shape = person.avatarShape,
          color = person.avatarColor,
          isProtected = person.isProtected,
          facts = loadedFacts,
          channels = loadedChannels,
        ),
      )
    }
  }

  fun onName(value: String) = _state.update { it.copy(name = value) }

  fun onAlias(value: String) {
    val alias = value.removePrefix("@")
    _state.update { it.copy(alias = alias) }
    viewModelScope.launch {
      val taken = alias.isNotBlank() && repository.isAliasTaken(alias, personId)
      _state.update { current -> if (current.alias == alias) current.copy(aliasTaken = taken) else current }
    }
  }

  fun onShape(shape: AvatarShape) = _state.update { it.copy(shape = shape) }

  fun onColor(color: AvatarColor) = _state.update { it.copy(color = color) }

  fun onProtected(value: Boolean) = _state.update { it.copy(isProtected = value) }

  fun onAddFact() = _state.update { it.copy(facts = it.facts + FactDraft(nextKey++, "", "")) }

  fun onFactChange(key: Long, label: String, value: String) = _state.update { state ->
    state.copy(facts = state.facts.map { if (it.key == key) it.copy(label = label, value = value) else it })
  }

  fun onRemoveFact(key: Long) = _state.update { state -> state.copy(facts = state.facts.filterNot { it.key == key }) }

  fun onMoveFact(from: Int, to: Int) = _state.update { state ->
    if (from !in state.facts.indices || to !in state.facts.indices) {
      state
    } else {
      state.copy(facts = state.facts.toMutableList().apply { add(to, removeAt(from)) })
    }
  }

  fun onSaveChannel(key: Long?, type: ChannelType, handle: String) = _state.update { state ->
    val cleanHandle = if (type == ChannelType.IN_PERSON) "" else handle.trim()
    if (key == null) {
      state.copy(channels = state.channels + ChannelDraft(nextKey++, type, cleanHandle))
    } else {
      state.copy(channels = state.channels.map { if (it.key == key) it.copy(type = type, handle = cleanHandle) else it })
    }
  }

  fun onRemoveChannel(key: Long) = _state.update { state -> state.copy(channels = state.channels.filterNot { it.key == key }) }

  fun delete(exportFirst: Boolean, onDeleted: () -> Unit) {
    val person = original ?: return
    viewModelScope.launch {
      if (exportFirst) exporter.buildText(personId)?.let { exporter.share(person.name, it) }
      repository.deletePerson(person)
      onDeleted()
    }
  }

  fun save(onSaved: (Long) -> Unit) {
    val current = _state.value
    if (!current.canSave) return
    viewModelScope.launch {
      val alias = current.alias.trim().takeIf { it.isNotBlank() }
      if (alias != null && repository.isAliasTaken(alias, personId)) {
        _state.update { it.copy(aliasTaken = true) }
        return@launch
      }
      val now = System.currentTimeMillis()
      val base = original
      val person = PersonEntity(
        id = personId,
        name = current.name.trim(),
        alias = alias,
        avatarShape = current.shape,
        avatarColor = current.color,
        isProtected = current.isProtected,
        archived = base?.archived ?: false,
        notes = base?.notes,
        createdAt = base?.createdAt ?: now,
        updatedAt = now,
      )
      val channels = current.channels.map {
        ChannelEntity(personId = 0, type = it.type, handle = it.handle.takeIf { handle -> handle.isNotBlank() })
      }
      val facts = current.facts
        .filter { it.label.isNotBlank() || it.value.isNotBlank() }
        .map { PinnedFactEntity(personId = 0, label = it.label.trim(), value = it.value.trim(), position = 0) }
      onSaved(repository.savePerson(person, channels, facts))
    }
  }
}

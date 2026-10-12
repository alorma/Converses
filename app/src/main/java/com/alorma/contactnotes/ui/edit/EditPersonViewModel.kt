package com.alorma.contactnotes.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.ui.person.PersonExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FactDraft(
  val key: Long,
  val label: String,
  val value: String,
)

data class EditSnapshot(
  val name: String = "",
  val alias: String = "",
  val shape: AvatarShape? = null,
  val color: AvatarColor? = null,
  val isProtected: Boolean = false,
  val facts: List<FactDraft> = emptyList(),
)

data class EditPersonUiState(
  val isNew: Boolean,
  val loaded: Boolean,
  val name: String = "",
  val alias: String = "",
  val shape: AvatarShape? = null,
  val color: AvatarColor? = null,
  val isProtected: Boolean = false,
  val facts: List<FactDraft> = emptyList(),
  val aliasTaken: Boolean = false,
  val entryCount: Int = 0,
  val baseline: EditSnapshot = EditSnapshot(),
) {
  private val snapshot: EditSnapshot
    get() = EditSnapshot(name, alias, shape, color, isProtected, facts)

  val canSave: Boolean get() = loaded && name.isNotBlank() && !aliasTaken && (isNew || snapshot != baseline)
}

/** Accions de l'editor; les implementa el `ViewModel` i les previews les deixen buides. */
interface EditPersonActions {
  fun onName(value: String)

  fun onAlias(value: String)

  fun onShape(shape: AvatarShape)

  fun onColor(color: AvatarColor?)

  fun onProtected(value: Boolean)

  fun onAddFact()

  fun onFactChange(
    key: Long,
    label: String,
    value: String,
  )

  fun onRemoveFact(key: Long)

  fun onMoveFact(
    from: Int,
    to: Int,
  )

  fun delete(
    exportFirst: Boolean,
    onDeleted: () -> Unit,
  )

  fun save(onSaved: (Long) -> Unit)
}

class EditPersonViewModel(
  private val personId: Long,
  private val repository: ConversesRepository,
  private val exporter: PersonExporter,
) : ViewModel(),
  EditPersonActions {
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
      val facts = repository.observePinnedFacts(personId).first()
      original = person
      val loadedFacts = facts.map { FactDraft(nextKey++, it.label, it.value) }
      _state.value =
        EditPersonUiState(
          isNew = false,
          loaded = true,
          name = person.name,
          alias = person.alias.orEmpty(),
          shape = person.avatarShape,
          color = person.avatarColor,
          isProtected = person.isProtected,
          facts = loadedFacts,
          entryCount = repository.getEntries(personId).size,
          baseline =
            EditSnapshot(
              name = person.name,
              alias = person.alias.orEmpty(),
              shape = person.avatarShape,
              color = person.avatarColor,
              isProtected = person.isProtected,
              facts = loadedFacts,
            ),
        )
    }
  }

  override fun onName(value: String) = _state.update { it.copy(name = value) }

  override fun onAlias(value: String) {
    val alias = value.removePrefix("@")
    _state.update { it.copy(alias = alias) }
    viewModelScope.launch {
      val taken = alias.isNotBlank() && repository.isAliasTaken(alias, personId)
      _state.update { current -> if (current.alias == alias) current.copy(aliasTaken = taken) else current }
    }
  }

  override fun onShape(shape: AvatarShape) = _state.update { it.copy(shape = shape) }

  override fun onColor(color: AvatarColor?) = _state.update { it.copy(color = color) }

  /** En una persona que ja existeix, el canvi es desa a l'acte; en una de nova, es desa amb el perfil. */
  override fun onProtected(value: Boolean) {
    val person = original
    if (person == null) {
      _state.update { it.copy(isProtected = value) }
      return
    }
    _state.update { it.copy(isProtected = value, baseline = it.baseline.copy(isProtected = value)) }
    val updated = person.copy(isProtected = value)
    original = updated
    viewModelScope.launch { repository.setProtected(updated, value) }
  }

  override fun onAddFact() = _state.update { it.copy(facts = it.facts + FactDraft(nextKey++, "", "")) }

  override fun onFactChange(
    key: Long,
    label: String,
    value: String,
  ) = _state.update { state ->
    state.copy(facts = state.facts.map { if (it.key == key) it.copy(label = label, value = value) else it })
  }

  override fun onRemoveFact(key: Long) =
    _state.update { state ->
      state.copy(
        facts =
          state.facts.filterNot {
            it.key ==
              key
          },
      )
    }

  override fun onMoveFact(
    from: Int,
    to: Int,
  ) = _state.update { state ->
    if (from !in state.facts.indices || to !in state.facts.indices) {
      state
    } else {
      state.copy(facts = state.facts.toMutableList().apply { add(to, removeAt(from)) })
    }
  }

  override fun delete(
    exportFirst: Boolean,
    onDeleted: () -> Unit,
  ) {
    val person = original ?: return
    viewModelScope.launch {
      if (exportFirst) exporter.buildText(personId)?.let { exporter.share(person.name, it) }
      repository.deletePerson(person)
      onDeleted()
    }
  }

  override fun save(onSaved: (Long) -> Unit) {
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
      val person =
        PersonEntity(
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
      val facts =
        current.facts
          .filter { it.label.isNotBlank() || it.value.isNotBlank() }
          .map { PinnedFactEntity(personId = 0, label = it.label.trim(), value = it.value.trim(), position = 0) }
      onSaved(repository.savePerson(person, facts))
    }
  }
}

package com.alorma.contactnotes.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.tracking.Actions
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskFormUiState(
  val people: List<PersonEntity> = emptyList(),
  val selected: PersonEntity? = null,
  /** Persona protegida triada que encara no s'ha autenticat; no es tria fins que es desbloqueja. */
  val pending: PersonEntity? = null,
  /** La tasca que s'edita; `null` si és nova. */
  val task: TaskEntity? = null,
  /** `false` fins que hi ha les persones i, en mode edició, la tasca. */
  val loaded: Boolean = false,
  /** En mode edició, la tasca ja no existeix (s'ha esborrat): la pantalla es tanca. */
  val missing: Boolean = false,
)

/**
 * Formulari «Nova tasca» i edició. S'hi arriba des del detall, on la persona ja s'ha desbloquejat: la inicial
 * (o la de la tasca) compta com a autenticada. Triar-ne una altra de protegida demana desbloquejar-la.
 */
class TaskFormViewModel(
  initialPersonId: Long,
  private val taskId: Long,
  private val repository: ConversesRepository,
  private val selectionBridge: PersonSelectionBridge,
  private val tracker: EventTracker,
  private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
  private val selectedId = MutableStateFlow<Long?>(initialPersonId.takeIf { it > 0 })
  private val pendingId = MutableStateFlow<Long?>(null)
  private val unlocked = MutableStateFlow(setOfNotNull(initialPersonId.takeIf { it > 0 }))
  private val task = MutableStateFlow<TaskEntity?>(null)
  private val taskState = MutableStateFlow(if (taskId > 0) TaskLoad.Loading else TaskLoad.Done)

  private enum class TaskLoad { Loading, Done, Missing }

  init {
    if (taskId > 0) {
      viewModelScope.launch {
        val loaded = repository.getTask(taskId)
        if (loaded == null) {
          taskState.value = TaskLoad.Missing
        } else {
          unlocked.update { it + loaded.personId }
          selectedId.value = loaded.personId
          task.value = loaded
          taskState.value = TaskLoad.Done
        }
      }
    }
    viewModelScope.launch {
      selectionBridge.picked.collect { pick ->
        if (pick != null) {
          // Una protegida creada ara ja s'ha autenticat a l'editor: no es demana una segona vegada
          if (pick.authenticated) unlocked.update { it + pick.personId }
          repository.observePerson(pick.personId).first()?.let(::select)
          selectionBridge.clear()
        }
      }
    }
  }

  private val people = repository.observePeople().map { items -> items.map { it.person } }
  private val selection =
    combine(selectedId, pendingId, unlocked) { id, pending, unlocked -> Triple(id, pending, unlocked) }

  val uiState: StateFlow<TaskFormUiState> =
    combine(people, selection, task, taskState) { people, (id, pending, unlocked), task, load ->
      TaskFormUiState(
        people = people,
        selected = people.firstOrNull { it.id == id }?.takeIf { !it.isProtected || it.id in unlocked },
        pending = people.firstOrNull { it.id == pending },
        task = task,
        loaded = load == TaskLoad.Done,
        missing = load == TaskLoad.Missing,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskFormUiState())

  /** Una protegida queda pendent d'autenticar; la resta es tria a l'acte. */
  fun select(person: PersonEntity) {
    if (person.isProtected && person.id !in unlocked.value) {
      pendingId.value = person.id
    } else {
      selectedId.value = person.id
      pendingId.value = null
    }
  }

  /** L'autenticació ha anat bé: la persona protegida passa a ser la triada. */
  fun unlock(personId: Long) {
    unlocked.update { it + personId }
    selectedId.value = personId
    pendingId.value = null
  }

  fun cancelPending() {
    pendingId.value = null
  }

  /** Crea la tasca o desa els canvis (conservant si ja estava feta). */
  fun save(
    personId: Long,
    text: String,
    dueDate: LocalDate?,
    onSaved: () -> Unit,
  ) {
    if (text.isBlank()) return
    tracker.trackAction(Actions.TaskSaved(creating = task.value == null, hasDue = dueDate != null))
    viewModelScope.launch {
      val due = dueDate?.toEpochDay()
      val existing = task.value
      repository.saveTask(
        existing?.copy(personId = personId, text = text, dueDate = due)
          ?: TaskEntity(personId = personId, text = text, dueDate = due, createdAt = clock()),
      )
      onSaved()
    }
  }

  fun delete(onDeleted: () -> Unit) {
    val existing = task.value ?: return
    tracker.trackAction(Actions.TaskDeleted())
    viewModelScope.launch {
      repository.deleteTask(existing)
      onDeleted()
    }
  }
}

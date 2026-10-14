package com.alorma.contactnotes.ui.entry

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ConversesSaveButton
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.initialsOf
import com.alorma.contactnotes.ui.navigation.isOnlyUrl
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.openSecuritySetup
import com.alorma.contactnotes.ui.person.promptUnlock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class QuickNoteUiState(
  val people: List<PersonEntity> = emptyList(),
  val selected: PersonEntity? = null,
  /** Persona protegida triada que encara no s'ha autenticat; no es tria fins que es desbloqueja. */
  val pending: PersonEntity? = null,
  val lastChannel: ChannelType? = null,
  /** `false` only before the first emission of [people]; an empty list is not «no people» until then. */
  val loaded: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class QuickNoteViewModel(
  initialPersonId: Long,
  private val repository: ConversesRepository,
  private val selectionBridge: PersonSelectionBridge,
) : ViewModel() {
  private val selectedId = MutableStateFlow<Long?>(null)
  private val pendingId = MutableStateFlow<Long?>(null)
  private val unlocked = MutableStateFlow<Set<Long>>(emptySet())

  init {
    if (initialPersonId > 0) request(initialPersonId)
    viewModelScope.launch {
      selectionBridge.picked.collect { pick ->
        if (pick != null) {
          // Una protegida creada ara ja s'ha autenticat a l'editor: no es demana una segona vegada
          if (pick.authenticated) unlocked.update { it + pick.personId }
          request(pick.personId)
          selectionBridge.clear()
        }
      }
    }
  }

  private val people = repository.observePeople().map { items -> items.map { it.person } }

  val uiState: StateFlow<QuickNoteUiState> =
    combine(people, selectedId, pendingId, unlocked) { people, id, pending, unlocked ->
      val selected = people.firstOrNull { it.id == id }?.takeIf { !it.isProtected || it.id in unlocked }
      Triple(people, selected, people.firstOrNull { it.id == pending })
    }.flatMapLatest { (people, selected, pending) ->
      if (selected == null) {
        flowOf(QuickNoteUiState(people = people, pending = pending))
      } else {
        repository.observeTimeline(selected.id).map { it.firstOrNull()?.channelType }.map { last ->
          QuickNoteUiState(people = people, selected = selected, pending = pending, lastChannel = last)
        }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickNoteUiState(loaded = false))

  /** Una protegida queda pendent d'autenticar; la resta es tria a l'acte. */
  fun select(person: PersonEntity) {
    if (person.isProtected && person.id !in unlocked.value) {
      pendingId.value = person.id
    } else {
      selectedId.value = person.id
      pendingId.value = null
    }
  }

  private fun request(id: Long) {
    viewModelScope.launch { repository.observePerson(id).first()?.let(::select) }
  }

  /** L'autenticació ha anat bé: la persona protegida passa a ser la triada. */
  fun unlock(personId: Long) {
    unlocked.update { it + personId }
    selectedId.value = personId
    pendingId.value = null
  }

  /** L'autenticació s'ha cancel·lat o ha fallat: la selecció es queda com estava. */
  fun cancelPending() {
    pendingId.value = null
  }

  fun save(
    personId: Long,
    kind: EntryKind,
    channel: ChannelType,
    text: String,
    occurredAt: Long?,
    onSaved: () -> Unit,
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
      onSaved()
    }
  }
}

/**
 * Default «Qui» of an imported text: a lone link is a note (`HAPPENED`); otherwise it is what the person
 * said (`HEARD`) once one is picked, and `SAID` before that (`HEARD` is disabled without a person).
 */
fun importedKind(
  text: String,
  hasPerson: Boolean,
): EntryKind =
  when {
    isOnlyUrl(text) -> EntryKind.HAPPENED
    hasPerson -> EntryKind.HEARD
    else -> EntryKind.SAID
  }

@Composable
fun QuickNoteScreen(
  personId: Long,
  onClose: () -> Unit,
  onNewPerson: () -> Unit,
  sharedText: String? = null,
  viewModel: QuickNoteViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val form =
    rememberEntryFormState(
      initialKind = sharedText?.let { importedKind(it, hasPerson = false) } ?: EntryKind.SAID,
      initialText = sharedText.orEmpty(),
    )
  val activity = LocalActivity.current as FragmentActivity
  var showNoLock by remember { mutableStateOf(false) }

  // Amb una persona protegida triada (o a punt de ser-ho) la pantalla no es pot capturar ni surt a Recents
  val secureWindow: SecureWindow = koinInject()
  val secure = state.selected?.isProtected == true || state.pending != null
  DisposableEffect(secure) {
    if (secure) secureWindow.acquire()
    onDispose { if (secure) secureWindow.release() }
  }

  val pending = state.pending
  val authTitle = stringResource(R.string.unlock_title, pending?.name.orEmpty())
  val authSubtitle = stringResource(R.string.unlock_subtitle)
  LaunchedEffect(pending?.id) {
    if (pending != null) {
      if (activity.canUnlockWithDevice()) {
        activity.promptUnlock(authTitle, authSubtitle, onError = { viewModel.cancelPending() }) {
          viewModel.unlock(pending.id)
        }
      } else {
        showNoLock = true
      }
    }
  }

  QuickNoteContent(
    state = state,
    form = form,
    importedText = sharedText,
    onClose = onClose,
    onSelectPerson = viewModel::select,
    onNewPerson = onNewPerson,
    onSave = { person, channel ->
      viewModel.save(person.id, form.kind, channel, form.text.trim(), form.occurredAt, onClose)
    },
  )

  if (showNoLock) {
    AlertDialog(
      onDismissRequest = {
        showNoLock = false
        viewModel.cancelPending()
      },
      title = { Text(stringResource(R.string.lock_none_title)) },
      text = { Text(stringResource(R.string.lock_none_body)) },
      confirmButton = {
        TextButton(onClick = { activity.openSecuritySetup() }) { Text(stringResource(R.string.lock_setup)) }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showNoLock = false
            viewModel.cancelPending()
          },
        ) { Text(stringResource(android.R.string.cancel)) }
      },
    )
  }
}

@Composable
fun QuickNoteContent(
  state: QuickNoteUiState,
  form: EntryFormState,
  onClose: () -> Unit,
  onSelectPerson: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  onSave: (person: PersonEntity, channel: ChannelType) -> Unit,
  importedText: String? = null,
) {
  val selected = state.selected
  val channel = form.channel ?: state.lastChannel ?: ChannelType.entries.first()
  var showSheet by rememberSaveable { mutableStateOf(false) }
  // El xip «Persona» obre el mateix selector; en triar-ne una, el xip queda seleccionat
  var pickedFromChip by rememberSaveable { mutableStateOf(false) }

  // Sense persones el selector estaria buit: es passa directe a crear-ne una (però no mentre encara es carreguen)
  val noPeople = state.loaded && state.people.isEmpty()
  val openPicker = {
    if (noPeople) {
      onNewPerson()
    } else if (state.loaded) {
      showSheet = true
    }
  }

  // Quan la persona arriba (triada, creada o desbloquejada) després d'haver tocat el xip, el xip es marca
  LaunchedEffect(selected?.id) {
    if (pickedFromChip && selected != null) {
      form.kind = EntryKind.HEARD
      pickedFromChip = false
    }
  }

  // Importat d'una altra app: en triar persona, «Qui» passa a la persona si l'usuari no l'havia tocat
  LaunchedEffect(selected?.id) {
    if (importedText != null && selected != null && !form.kindTouched) {
      form.kind = importedKind(importedText, hasPerson = true)
    }
  }

  PersonTheme(selected?.color) {
    val scrollBehavior = rememberTopBarScrollBehavior()
    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = MaterialTheme.colorScheme.surface,
      contentWindowInsets = WindowInsets.navigationBars,
      topBar = {
        ConversesTopBar(
          title = stringResource(R.string.quick_note),
          scrollBehavior = scrollBehavior,
          navigationIcon = {
            FilledTonalIconButton(onClick = onClose) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
            }
          },
          actions = {
            ConversesSaveButton(
              onClick = { if (selected != null) onSave(selected, channel) },
              enabled = form.canSave && selected != null,
              modifier = Modifier.padding(end = 8.dp),
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
          },
        )
      },
    ) { padding ->
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        EntryForm(
          state = form,
          person = selected,
          channel = channel,
          onChannelChange = { form.channel = it },
          onPickPerson = {
            pickedFromChip = true
            openPicker()
          },
          header = {
            PersonPicker(
              selected = selected,
              noPeople = noPeople,
              onClick = {
                pickedFromChip = false
                openPicker()
              },
            )
          },
        )
      }
    }
    if (showSheet) {
      PersonPickerSheet(
        people = state.people,
        selectedId = selected?.id,
        onSelect = {
          onSelectPerson(it)
          showSheet = false
        },
        onNewPerson = {
          showSheet = false
          onNewPerson()
        },
        onDismiss = {
          pickedFromChip = false
          showSheet = false
        },
      )
    }
  }
}

@Composable
private fun PersonPicker(
  selected: PersonEntity?,
  noPeople: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.extraLarge,
    color = GroupedListDefaults.containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (selected != null) {
        Avatar(initialsOf(selected.name), selected.shape, selected.color, size = 44.dp, locked = selected.isProtected)
      } else {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Rounded.PersonOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.quick_with_whom).uppercase(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = selected?.name ?: stringResource(if (noPeople) R.string.create_first_person else R.string.pick_person),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        )
      }
      Icon(Icons.Rounded.UnfoldMore, contentDescription = null)
    }
  }
}

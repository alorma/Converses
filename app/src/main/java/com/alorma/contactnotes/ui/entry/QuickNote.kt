package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.ui.people.initialsOf
import com.alorma.contactnotes.ui.people.label
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class QuickNoteUiState(
  val people: List<PersonEntity> = emptyList(),
  val selected: PersonEntity? = null,
  val channels: List<ChannelType> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class QuickNoteViewModel(
  initialPersonId: Long,
  private val repository: ConversesRepository,
) : ViewModel() {
  private val selectedId = MutableStateFlow(initialPersonId.takeIf { it > 0 })

  private val people = repository.observePeople().map { items -> items.map { it.person } }

  val uiState: StateFlow<QuickNoteUiState> = combine(people, selectedId) { people, id ->
    people to (people.firstOrNull { it.id == id } ?: people.firstOrNull())
  }.flatMapLatest { (people, selected) ->
    val channels = if (selected == null || selected.isProtected) {
      flowOf(emptyList())
    } else {
      repository.observeChannels(selected.id).map { list -> list.map { it.type } }
    }
    channels.map { QuickNoteUiState(people = people, selected = selected, channels = it) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickNoteUiState())

  fun select(person: PersonEntity) {
    selectedId.value = person.id
  }

  fun save(personId: Long, kind: EntryKind, channel: ChannelType, text: String, occurredAt: Long?, onSaved: () -> Unit) {
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

@Composable
fun QuickNoteScreen(
  personId: Long,
  onClose: () -> Unit,
  viewModel: QuickNoteViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val form = rememberEntryFormState()
  val selected = state.selected
  val available = availableChannels(state.channels, showAll = selected?.isProtected == true)
  val channel = form.channel?.takeIf { it in available } ?: available.first()

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onClose) {
          Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
        }
        Button(
          onClick = {
            if (selected != null) {
              viewModel.save(selected.id, form.kind, channel, form.text.trim(), form.occurredAt, onClose)
            }
          },
          enabled = form.canSave && selected != null,
        ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
      }
      Text(stringResource(R.string.quick_note), style = ScreenTitleStyle, modifier = Modifier.padding(start = 4.dp))

      if (selected != null) {
        PersonPicker(selected = selected, people = state.people, onSelect = viewModel::select)
      }
      Text(
        text = stringResource(R.string.quick_where),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
      )
      ChannelChips(channels = available, selected = channel, onSelected = { form.channel = it })
      KindSelector(form)
      EntryTextField(form, label = stringResource(form.kind.quickLabel()))
      Row { TimeChip(occurredAt = form.occurredAt, onChange = { form.occurredAt = it }) }
    }
  }
}

private fun EntryKind.quickLabel(): Int =
  if (this == EntryKind.HAPPENED) R.string.entry_label_quick_happened else R.string.entry_label_quick_said

@Composable
private fun PersonPicker(selected: PersonEntity, people: List<PersonEntity>, onSelect: (PersonEntity) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = GroupedListDefaults.containerColor,
      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Avatar(initialsOf(selected.name), selected.avatarShape, selected.avatarColor, size = 44.dp, locked = selected.isProtected)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.quick_with_whom).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(selected.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Rounded.UnfoldMore, contentDescription = null)
      }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      people.forEach { person ->
        DropdownMenuItem(
          text = { Text(person.name) },
          leadingIcon = {
            Avatar(initialsOf(person.name), person.avatarShape, person.avatarColor, size = 32.dp, locked = person.isProtected)
          },
          trailingIcon = if (person.isProtected) {
            { Icon(Icons.Rounded.Lock, contentDescription = null) }
          } else {
            null
          },
          onClick = {
            onSelect(person)
            expanded = false
          },
        )
      }
    }
  }
}

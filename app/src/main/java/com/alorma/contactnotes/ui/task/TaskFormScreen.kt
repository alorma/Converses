package com.alorma.contactnotes.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.designsystem.components.ConversesSaveButton
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.ui.dateFormatter
import com.alorma.contactnotes.ui.entry.FormSection
import com.alorma.contactnotes.ui.entry.FormTextField
import com.alorma.contactnotes.ui.entry.PasteButton
import com.alorma.contactnotes.ui.entry.PendingPersonUnlock
import com.alorma.contactnotes.ui.entry.PersonPicker
import com.alorma.contactnotes.ui.entry.PersonPickerSheet
import com.alorma.contactnotes.ui.entry.SecureWhile
import com.alorma.contactnotes.ui.entry.SelectableChip
import com.alorma.contactnotes.ui.entry.pasteInto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Estat editable del formulari de tasca: el text i el dia previst (`null` és «Sense data»). */
class TaskFormState(
  text: String = "",
  dueDate: LocalDate? = null,
) {
  var text by mutableStateOf(text)
  var dueDate by mutableStateOf(dueDate)
  val canSave: Boolean get() = text.isNotBlank()

  companion object {
    val Saver: Saver<TaskFormState, Any> =
      listSaver(
        save = { listOf(it.text, it.dueDate?.toEpochDay()) },
        restore = { TaskFormState(it[0] as String, (it[1] as Long?)?.let(LocalDate::ofEpochDay)) },
      )
  }
}

/** En mode edició, l'estat es crea de nou quan arriba la tasca. */
@Composable
fun rememberTaskFormState(task: TaskEntity?): TaskFormState =
  rememberSaveable(task?.id, saver = TaskFormState.Saver) {
    TaskFormState(task?.text.orEmpty(), task?.dueDate?.let(LocalDate::ofEpochDay))
  }

@Composable
fun TaskFormScreen(
  personId: Long,
  taskId: Long,
  onClose: () -> Unit,
  onNewPerson: () -> Unit,
  viewModel: TaskFormViewModel = koinViewModel(parameters = { parametersOf(personId, taskId) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  LaunchedEffect(state.missing) { if (state.missing) onClose() }

  SecureWhile(state.selected?.isProtected == true || state.pending != null)
  PendingPersonUnlock(state.pending, onUnlock = viewModel::unlock, onCancel = viewModel::cancelPending)

  val form = rememberTaskFormState(state.task)
  TaskFormContent(
    state = state,
    form = form,
    isNew = taskId <= 0,
    today = LocalDate.now(),
    onClose = onClose,
    onSelectPerson = viewModel::select,
    onNewPerson = onNewPerson,
    onSave = { person -> viewModel.save(person.id, form.text.trim(), form.dueDate, onClose) },
    onDelete = { viewModel.delete(onClose) },
    focusText = taskId <= 0,
  )
}

@Composable
fun TaskFormContent(
  state: TaskFormUiState,
  form: TaskFormState,
  isNew: Boolean,
  today: LocalDate,
  onClose: () -> Unit,
  onSelectPerson: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  onSave: (PersonEntity) -> Unit,
  onDelete: () -> Unit,
  /** Posa el focus al text en obrir (nova tasca); les previews no ho fan. */
  focusText: Boolean = false,
) {
  val selected = state.selected
  var showPeople by rememberSaveable { mutableStateOf(false) }
  var confirmDelete by rememberSaveable { mutableStateOf(false) }
  val noPeople = state.loaded && state.people.isEmpty()

  PersonTheme(selected?.color) {
    val scrollBehavior = rememberTopBarScrollBehavior()
    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = MaterialTheme.colorScheme.surface,
      contentWindowInsets = WindowInsets.navigationBars,
      topBar = {
        ConversesTopBar(
          title = stringResource(if (isNew) R.string.task_new else R.string.task_edit),
          scrollBehavior = scrollBehavior,
          navigationIcon = {
            FilledTonalIconButton(onClick = onClose) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
            }
          },
          actions = {
            if (!isNew) {
              IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.task_delete))
              }
            }
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
            .imePadding(),
      ) {
        Column(
          modifier =
            Modifier
              .weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          PersonPicker(
            selected = selected,
            noPeople = noPeople,
            onClick = {
              if (noPeople) {
                onNewPerson()
              } else if (state.loaded) {
                showPeople = true
              }
            },
          )
          FormTextField(
            value = form.text,
            onValueChange = { form.text = it },
            placeholder = stringResource(R.string.task_text_hint),
            requestFocus = focusText,
            footer = { PasteButton(onPasted = { form.text = pasteInto(form.text, it) }) },
          )
          FormSection(title = stringResource(R.string.entry_when_title)) {
            DueChips(dueDate = form.dueDate, today = today, onChange = { form.dueDate = it })
          }
        }
        // Fix a baix, fora del que es desplaça, perquè el teclat no el tapi
        ConversesSaveButton(
          onClick = { if (selected != null) onSave(selected) },
          enabled = form.canSave && selected != null,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
        ) {
          Icon(Icons.Rounded.Flag, contentDescription = null)
          Spacer(Modifier.padding(horizontal = 4.dp))
          Text(
            stringResource(if (isNew) R.string.task_save else R.string.save_changes),
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }

    if (showPeople) {
      PersonPickerSheet(
        people = state.people,
        selectedId = selected?.id,
        onSelect = {
          onSelectPerson(it)
          showPeople = false
        },
        onNewPerson = {
          showPeople = false
          onNewPerson()
        },
        onDismiss = { showPeople = false },
      )
    }
    if (confirmDelete) {
      AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.task_delete_title)) },
        confirmButton = {
          TextButton(onClick = {
            confirmDelete = false
            onDelete()
          }) { Text(stringResource(R.string.delete_button)) }
        },
        dismissButton = {
          TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
        },
      )
    }
  }
}

/** «Quan»: xips d'una sola selecció cap al futur; «Tria dia…» mostra la data triada quan no és cap dels fixos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DueChips(
  dueDate: LocalDate?,
  today: LocalDate,
  onChange: (LocalDate?) -> Unit,
) {
  // El de la configuració (també a les previews localitzades), no el del procés
  val locale = LocalConfiguration.current.locales[0]
  val haptics = rememberConversesHaptics()
  var showPicker by rememberSaveable { mutableStateOf(false) }
  val preset = presetOf(dueDate, today)
  val pick = { date: LocalDate? ->
    if (date != dueDate) haptics.segmentTick()
    onChange(date)
  }
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    DuePreset.entries.forEach { option ->
      SelectableChip(
        label =
          when (option) {
            DuePreset.None -> stringResource(R.string.task_no_date)
            DuePreset.Today -> stringResource(R.string.today)
            DuePreset.Tomorrow -> stringResource(R.string.task_tomorrow)
            DuePreset.RelevantDay -> dayName(relevantDay(today), locale)
            DuePreset.NextWeek -> stringResource(R.string.task_next_week)
          },
        selected = option == preset,
        onClick = { pick(option.date(today)) },
      )
    }
    val custom = dueDate != null && preset == null
    SelectableChip(
      label = if (custom) formatDueDate(dueDate, locale) else stringResource(R.string.task_pick_day),
      selected = custom,
      leadingIcon = Icons.Rounded.EditCalendar,
      onClick = { showPicker = true },
    )
  }
  if (showPicker) {
    FutureDatePicker(
      initial = dueDate?.takeIf { it >= today } ?: today,
      today = today,
      onDismiss = { showPicker = false },
      onPicked = {
        pick(it)
        showPicker = false
      },
    )
  }
}

/** «Dc. 19 de nov.»: la data d'un xip o etiqueta de tasca. */
fun formatDueDate(
  date: LocalDate,
  locale: Locale = Locale.getDefault(),
): String = date.format(dateFormatter("EEE d MMM", locale)).replaceFirstChar { it.titlecase(locale) }

/** Selector de dia a partir d'avui (les tasques miren cap al futur). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FutureDatePicker(
  initial: LocalDate,
  today: LocalDate,
  onDismiss: () -> Unit,
  onPicked: (LocalDate) -> Unit,
) {
  val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
  val state =
    rememberDatePickerState(
      initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
      selectableDates =
        object : SelectableDates {
          override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc

          override fun isSelectableYear(year: Int): Boolean = year >= today.year
        },
    )
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        enabled = state.selectedDateMillis != null,
        onClick = {
          state.selectedDateMillis?.let {
            onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
          }
        },
      ) { Text(stringResource(android.R.string.ok)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
  ) { DatePicker(state = state) }
}

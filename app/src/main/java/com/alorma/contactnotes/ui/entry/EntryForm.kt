package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.designsystem.components.ConversesFilterChip
import com.alorma.contactnotes.designsystem.components.ConversesToggleGroup
import com.alorma.contactnotes.designsystem.components.ToggleOption
import com.alorma.contactnotes.ui.people.label
import com.alorma.contactnotes.ui.person.dayLabel
import com.alorma.contactnotes.ui.person.entryDate
import com.alorma.contactnotes.ui.person.formatTime
import com.alorma.contactnotes.ui.person.icon
import com.alorma.contactnotes.ui.person.label
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Estat editable d'una entrada nova. `occurredAt == null` vol dir "ara" en desar. */
class EntryFormState(
  kind: EntryKind = EntryKind.SAID,
  channel: ChannelType? = null,
  text: String = "",
  occurredAt: Long? = null,
) {
  var kind by mutableStateOf(kind)
  var channel by mutableStateOf(channel)
  var text by mutableStateOf(text)
  var occurredAt by mutableStateOf(occurredAt)

  val canSave: Boolean get() = text.isNotBlank()

  companion object {
    val Saver: Saver<EntryFormState, Any> = listSaver(
      save = { listOf(it.kind.name, it.channel?.name, it.text, it.occurredAt) },
      restore = {
        EntryFormState(
          kind = EntryKind.valueOf(it[0] as String),
          channel = (it[1] as String?)?.let(ChannelType::valueOf),
          text = it[2] as String,
          occurredAt = it[3] as Long?,
        )
      },
    )
  }
}

@Composable
fun rememberEntryFormState(initial: EntryEntity? = null): EntryFormState =
  rememberSaveable(initial?.id, saver = EntryFormState.Saver) {
    EntryFormState(
      kind = initial?.kind ?: EntryKind.SAID,
      channel = initial?.channelType,
      text = initial?.text.orEmpty(),
      occurredAt = initial?.occurredAt,
    )
  }

/** Canals que es poden triar: els de la persona (o tots si no es poden mostrar) més "Altres". */
fun availableChannels(personChannels: List<ChannelType>, showAll: Boolean = false): List<ChannelType> =
  if (showAll) ChannelType.entries else (personChannels.distinct() + ChannelType.OTHER).distinct()

@Composable
fun KindSelector(state: EntryFormState, modifier: Modifier = Modifier) {
  ConversesToggleGroup(
    options = EntryKind.entries.map { ToggleOption(it, stringResource(it.label()), it.icon()) },
    selected = state.kind,
    onSelect = { state.kind = it },
    modifier = modifier,
  )
}

@Composable
fun EntryTextField(state: EntryFormState, label: String, modifier: Modifier = Modifier) {
  TextField(
    value = state.text,
    onValueChange = { state.text = it },
    modifier = modifier.fillMaxWidth().heightIn(min = 128.dp),
    label = { Text(label) },
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    ),
  )
}

@Composable
fun ChannelChips(
  channels: List<ChannelType>,
  selected: ChannelType,
  onSelected: (ChannelType) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(channels, key = { it.name }) { channel ->
      ConversesFilterChip(
        label = stringResource(channel.label()),
        selected = channel == selected,
        onClick = { onSelected(channel) },
        tonal = true,
      )
    }
  }
}

/** Xip amb l'hora; en tocar-lo s'obre el selector de data i després el d'hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeChip(occurredAt: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
  var step by remember { mutableStateOf(PickerStep.None) }
  var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

  val label = if (occurredAt == null) {
    stringResource(R.string.time_now)
  } else {
    "${dayLabel(entryDate(occurredAt))}, ${formatTime(occurredAt)}"
  }
  AssistChip(
    onClick = { step = PickerStep.Date },
    label = { Text(label) },
    leadingIcon = { Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
    shape = MaterialTheme.shapes.large,
    modifier = modifier,
  )

  when (step) {
    PickerStep.None -> Unit
    PickerStep.Date -> {
      val state = rememberDatePickerState(initialSelectedDateMillis = occurredAt ?: System.currentTimeMillis())
      DatePickerDialog(
        onDismissRequest = { step = PickerStep.None },
        confirmButton = {
          TextButton(onClick = {
            pickedDateMillis = state.selectedDateMillis
            step = PickerStep.Time
          }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
          TextButton(onClick = {
            onChange(null)
            step = PickerStep.None
          }) { Text(stringResource(R.string.time_now)) }
        },
      ) { DatePicker(state = state) }
    }
    PickerStep.Time -> {
      val zone = ZoneId.systemDefault()
      val initial = Instant.ofEpochMilli(occurredAt ?: System.currentTimeMillis()).atZone(zone)
      val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { step = PickerStep.None },
        confirmButton = {
          TextButton(onClick = {
            val date = Instant.ofEpochMilli(pickedDateMillis ?: System.currentTimeMillis()).atZone(ZoneId.of("UTC")).toLocalDate()
            val at = LocalDateTime.of(date, java.time.LocalTime.of(state.hour, state.minute)).atZone(zone).toInstant().toEpochMilli()
            onChange(at)
            step = PickerStep.None
          }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
          TextButton(onClick = { step = PickerStep.None }) { Text(stringResource(android.R.string.cancel)) }
        },
        text = { TimePicker(state = state) },
      )
    }
  }
}

private enum class PickerStep { None, Date, Time }

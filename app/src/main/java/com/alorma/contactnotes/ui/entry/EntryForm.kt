package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.designsystem.components.ConversesToggleGroup
import com.alorma.contactnotes.designsystem.components.ToggleOption
import com.alorma.contactnotes.ui.people.label
import com.alorma.contactnotes.ui.person.icon
import com.alorma.contactnotes.ui.person.label

/** Estat editable d'una entrada. `whenChoice == Now` vol dir «ara» en desar. */
class EntryFormState(
  kind: EntryKind = EntryKind.SAID,
  channel: ChannelType? = null,
  text: String = "",
  whenChoice: WhenChoice = WhenChoice.Now,
) {
  var kind by mutableStateOf(kind)
  var channel by mutableStateOf(channel)
  var text by mutableStateOf(text)
  var whenChoice by mutableStateOf(whenChoice)

  val occurredAt: Long? get() = whenChoice.at
  val canSave: Boolean get() = text.isNotBlank()

  companion object {
    val Saver: Saver<EntryFormState, Any> = listSaver(
      save = { listOf(it.kind.name, it.channel?.name, it.text, WhenChoice.encode(it.whenChoice)) },
      restore = {
        @Suppress("UNCHECKED_CAST")
        EntryFormState(
          kind = EntryKind.valueOf(it[0] as String),
          channel = (it[1] as String?)?.let(ChannelType::valueOf),
          text = it[2] as String,
          whenChoice = WhenChoice.decode(it[3] as List<Any?>),
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
      whenChoice = initial?.let { WhenChoice.Stored(it.occurredAt) } ?: WhenChoice.Now,
    )
  }

/** Canals que es poden triar: els de la persona (o tots si no es poden mostrar) més "Altres". */
fun availableChannels(personChannels: List<ChannelType>, showAll: Boolean = false): List<ChannelType> =
  if (showAll) ChannelType.entries else (personChannels.distinct() + ChannelType.OTHER).distinct()

/**
 * Formulari d'entrada comú: Tipus, text, On i Quan.
 * `header` va a dalt de tot (la targeta «Amb qui» de la nota ràpida) i `saveButton` a baix.
 */
@Composable
fun EntryForm(
  state: EntryFormState,
  channels: List<ChannelType>,
  channel: ChannelType,
  onChannelChange: (ChannelType) -> Unit,
  modifier: Modifier = Modifier,
  header: @Composable () -> Unit = {},
  saveButton: @Composable () -> Unit = {},
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
    header()
    KindSelector(state)
    EntryTextField(state, placeholder = stringResource(state.kind.promptLabel()))
    FormSection(title = stringResource(R.string.entry_where_title)) {
      ChannelChips(channels = channels, selected = channel, onSelected = onChannelChange)
    }
    FormSection(title = stringResource(R.string.entry_when_title)) {
      WhenRow(choice = state.whenChoice, onChoice = { state.whenChoice = it })
    }
    saveButton()
  }
}

@Composable
private fun FormSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 6.dp),
    )
    content()
  }
}

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
fun EntryTextField(state: EntryFormState, placeholder: String, modifier: Modifier = Modifier) {
  val container = MaterialTheme.colorScheme.surfaceContainerHigh
  TextField(
    value = state.text,
    onValueChange = { state.text = it },
    modifier = modifier.fillMaxWidth().heightIn(min = 120.dp),
    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
    placeholder = { Text(placeholder, fontSize = 17.sp) },
    shape = RoundedCornerShape(24.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = container,
      unfocusedContainerColor = container,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      disabledIndicatorColor = Color.Transparent,
      errorIndicatorColor = Color.Transparent,
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
  val scheme = MaterialTheme.colorScheme
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    channels.forEach { channel ->
      val isSelected = channel == selected
      FilterChip(
        selected = isSelected,
        onClick = { onSelected(channel) },
        modifier = Modifier.height(36.dp),
        label = { Text(stringResource(channel.label())) },
        leadingIcon = if (isSelected) {
          { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
          null
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = scheme.primaryContainer,
          selectedLabelColor = scheme.onPrimaryContainer,
          selectedLeadingIconColor = scheme.onPrimaryContainer,
        ),
        border = if (isSelected) null else BorderStroke(1.dp, scheme.outlineVariant),
      )
    }
  }
}

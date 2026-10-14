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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.designsystem.components.ConversesToggleGroup
import com.alorma.contactnotes.designsystem.components.ToggleOption
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.ui.people.label
import com.alorma.contactnotes.ui.person.PersonInitialAvatar
import com.alorma.contactnotes.ui.person.channelName
import com.alorma.contactnotes.ui.person.firstName
import com.alorma.contactnotes.ui.person.whoIcon
import com.alorma.contactnotes.ui.person.whoLabel

/** Estat editable d'una entrada. `whenChoice == Now` vol dir «ara» en desar. */
class EntryFormState(
  kind: EntryKind = EntryKind.SAID,
  channel: ChannelType? = null,
  text: String = "",
  whenChoice: WhenChoice = WhenChoice.Now,
  kindTouched: Boolean = false,
) {
  var kind by mutableStateOf(kind)
  var channel by mutableStateOf(channel)
  var text by mutableStateOf(text)
  var whenChoice by mutableStateOf(whenChoice)

  /** `true` once the user picks «Qui» by hand, so an automatic default never overrides it. */
  var kindTouched by mutableStateOf(kindTouched)

  val occurredAt: Long? get() = whenChoice.at
  val canSave: Boolean get() = text.isNotBlank()

  companion object {
    val Saver: Saver<EntryFormState, Any> =
      listSaver(
        save = { listOf(it.kind.name, it.channel?.name, it.text, WhenChoice.encode(it.whenChoice), it.kindTouched) },
        restore = {
          @Suppress("UNCHECKED_CAST")
          EntryFormState(
            kind = EntryKind.valueOf(it[0] as String),
            channel = (it[1] as String?)?.let(ChannelType::valueOf),
            text = it[2] as String,
            whenChoice = WhenChoice.decode(it[3] as List<Any?>),
            kindTouched = it[4] as Boolean,
          )
        },
      )
  }
}

@Composable
fun rememberEntryFormState(
  initial: EntryEntity? = null,
  initialKind: EntryKind = EntryKind.SAID,
  initialText: String = "",
): EntryFormState =
  rememberSaveable(initial?.id, saver = EntryFormState.Saver) {
    EntryFormState(
      kind = initial?.kind ?: initialKind,
      channel = initial?.channelType,
      text = initial?.text ?: initialText,
      whenChoice = initial?.let { WhenChoice.Stored(it.occurredAt) } ?: WhenChoice.Now,
    )
  }

/**
 * Formulari d'entrada comú: Qui, text, On i Quan.
 * `header` va a dalt de tot (la targeta «Amb qui» de la nota ràpida) i `saveButton` a baix.
 */
@Composable
fun EntryForm(
  state: EntryFormState,
  person: PersonEntity?,
  channel: ChannelType,
  onChannelChange: (ChannelType) -> Unit,
  modifier: Modifier = Modifier,
  focusText: Boolean = false,
  onPickPerson: (() -> Unit)? = null,
  header: @Composable () -> Unit = {},
  textFooter: @Composable () -> Unit = {},
  saveButton: @Composable () -> Unit = {},
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
    header()
    FormSection(title = stringResource(R.string.entry_who_title)) {
      WhoSelector(state, person, onPickPerson = onPickPerson)
    }
    EntryTextField(
      state,
      placeholder = entryHint(state.kind, person?.name?.let(::firstName).orEmpty()),
      requestFocus = focusText,
      footer = textFooter,
    )
    FormSection(title = stringResource(R.string.entry_where_title)) {
      ChannelChips(selected = channel, onSelected = onChannelChange)
    }
    FormSection(title = stringResource(R.string.entry_when_title)) {
      WhenRow(choice = state.whenChoice, onChoice = { state.whenChoice = it })
    }
    saveButton()
  }
}

@Composable
private fun FormSection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
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
fun entryHint(
  kind: EntryKind,
  firstName: String,
): String =
  when (kind) {
    EntryKind.SAID -> stringResource(R.string.entry_hint_me)
    EntryKind.HEARD -> stringResource(R.string.entry_hint_person, firstName)
    EntryKind.HAPPENED -> stringResource(R.string.entry_hint_note)
  }

/** Selector «Qui»: Jo, la persona (amb el seu avatar) o un apunt. */
@Composable
fun WhoSelector(
  state: EntryFormState,
  person: PersonEntity?,
  modifier: Modifier = Modifier,
  onPickPerson: (() -> Unit)? = null,
) {
  val name = person?.let { firstName(it.name) }
  ConversesToggleGroup(
    options =
      EntryKind.entries.map { kind ->
        when (kind) {
          EntryKind.HEARD ->
            ToggleOption(
              value = kind,
              label = name ?: stringResource(R.string.entry_who_person),
              leading = if (person != null) ({ PersonInitialAvatar(person, size = 26.dp) }) else null,
              enabled = person != null || onPickPerson != null,
              contentDescription = person?.name,
            )
          else ->
            ToggleOption(
              value = kind,
              label = whoLabel(kind, name.orEmpty()),
              icon = kind.whoIcon(selected = false),
              selectedIcon = kind.whoIcon(selected = true),
            )
        }
      },
    selected = state.kind,
    onSelect = { kind ->
      if (kind == EntryKind.HEARD && person == null) {
        onPickPerson?.invoke()
      } else {
        state.kind = kind
        state.kindTouched = true
      }
    },
    modifier = modifier,
    prominent = true,
  )
}

@Composable
fun EntryTextField(
  state: EntryFormState,
  placeholder: String,
  modifier: Modifier = Modifier,
  requestFocus: Boolean = false,
  /** Va dins del contenidor del camp, sota el text (p. ex. el xip «Enganxa»). */
  footer: @Composable () -> Unit = {},
) {
  val focusRequester = remember { FocusRequester() }
  if (requestFocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column {
      TextField(
        value = state.text,
        onValueChange = { state.text = it },
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
        placeholder = { Text(placeholder, fontSize = 17.sp) },
        shape = RoundedCornerShape(24.dp),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
          ),
      )
      footer()
    }
  }
}

/** Els 5 grups, sempre en l'ordre fix: l'entrada es pot etiquetar amb qualsevol, tingui o no la persona entrades al grup. */
@Composable
fun ChannelChips(
  selected: ChannelType,
  onSelected: (ChannelType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val haptics = rememberConversesHaptics()
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ChannelType.entries.forEach { channel ->
      val isSelected = channel == selected
      FilterChip(
        selected = isSelected,
        onClick = {
          if (!isSelected) haptics.segmentTick()
          onSelected(channel)
        },
        modifier = Modifier.height(36.dp),
        label = { Text(channelName(channel)) },
        leadingIcon =
          if (isSelected) {
            {
              Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
              )
            }
          } else {
            null
          },
        shape = RoundedCornerShape(12.dp),
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = scheme.primaryContainer,
            selectedLabelColor = scheme.onPrimaryContainer,
            selectedLeadingIconColor = scheme.onPrimaryContainer,
          ),
        border = if (isSelected) null else BorderStroke(1.dp, scheme.outlineVariant),
      )
    }
  }
}

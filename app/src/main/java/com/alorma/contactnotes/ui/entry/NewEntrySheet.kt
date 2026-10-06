package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.theme.SheetTitleStyle
import com.alorma.contactnotes.ui.people.initialsOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntrySheet(
  person: PersonEntity,
  channels: List<ChannelType>,
  onDismiss: () -> Unit,
  initial: EntryEntity? = null,
  onSave: (kind: EntryKind, channel: ChannelType, text: String, occurredAt: Long?) -> Unit,
) {
  val form = rememberEntryFormState(initial)
  val available = availableChannels(channels)
  val channel = form.channel?.takeIf { it in available } ?: available.first()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(initials = initialsOf(person.name), shape = person.avatarShape, color = person.avatarColor, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(stringResource(if (initial == null) R.string.new_entry else R.string.edit_entry), style = SheetTitleStyle)
          Text(
            text = stringResource(R.string.with_person, person.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalIconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
        }
      }
      KindSelector(form)
      EntryTextField(form, label = stringResource(form.kind.promptLabel()))
      ChannelChips(channels = available, selected = channel, onSelected = { form.channel = it })
      Row { TimeChip(occurredAt = form.occurredAt, onChange = { form.occurredAt = it }) }
      Button(
        onClick = { onSave(form.kind, channel, form.text.trim(), form.occurredAt) },
        enabled = form.canSave,
        modifier = Modifier.fillMaxWidth().height(56.dp),
      ) {
        Icon(Icons.Rounded.Check, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(
          stringResource(if (initial == null) R.string.save_to_timeline else R.string.save_changes),
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}

fun EntryKind.promptLabel(): Int = when (this) {
  EntryKind.SAID -> R.string.entry_label_said
  EntryKind.HEARD -> R.string.entry_label_heard
  EntryKind.HAPPENED -> R.string.entry_label_happened
}

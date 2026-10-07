package com.alorma.contactnotes.ui.person

import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.data.db.color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.ui.people.initialsOf
import com.alorma.contactnotes.ui.people.label
import java.time.LocalDate

/** Full d'accions d'una entrada (PNG 5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryActionsSheet(
  entry: EntryEntity,
  firstName: String,
  otherHandle: String?,
  dayCount: Int,
  onDismiss: () -> Unit,
  onEdit: () -> Unit,
  onCopy: () -> Unit,
  onMove: () -> Unit,
  onDelete: () -> Unit,
  onDeleteDay: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    EntryActionsContent(
      entry = entry,
      firstName = firstName,
      otherHandle = otherHandle,
      dayCount = dayCount,
      onEdit = onEdit,
      onCopy = onCopy,
      onMove = onMove,
      onDelete = onDelete,
      onDeleteDay = onDeleteDay,
    )
  }
}

/** Contingut del full d'accions; el `ModalBottomSheet` és una finestra a part. */
@Composable
fun EntryActionsContent(
  entry: EntryEntity,
  firstName: String,
  otherHandle: String?,
  dayCount: Int,
  onEdit: () -> Unit,
  onCopy: () -> Unit,
  onMove: () -> Unit,
  onDelete: () -> Unit,
  onDeleteDay: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 16.dp)
      .padding(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = listOf(
        whoLabel(entry.kind, firstName),
        formatTime(entry.occurredAt),
        channelName(entry.channelType, otherHandle),
      ).joinToString(" · ") { it.uppercase() },
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp),
    )
    Text(
      text = entry.text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
    )
    ActionRow(Icons.Rounded.Edit, stringResource(R.string.entry_edit), null, 0, 3, onEdit)
    ActionRow(Icons.Rounded.ContentCopy, stringResource(R.string.entry_copy), null, 1, 3, onCopy)
    ActionRow(Icons.Rounded.Logout, stringResource(R.string.entry_move), null, 2, 3, onMove)
    val canDeleteDay = dayCount > 1
    val isToday = entryDate(entry.occurredAt) == LocalDate.now()
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      ActionRow(
        icon = Icons.Rounded.Delete,
        title = stringResource(R.string.entry_delete),
        subtitle = null,
        index = 0,
        count = if (canDeleteDay) 2 else 1,
        onClick = onDelete,
        destructive = true,
      )
      if (canDeleteDay) {
        ActionRow(
          icon = Icons.Rounded.DeleteSweep,
          title = stringResource(R.string.entry_delete_day),
          subtitle = pluralStringResource(
            if (isToday) R.plurals.entry_delete_today_summary else R.plurals.entry_delete_day_summary,
            dayCount,
            dayCount,
            channelName(entry.channelType, otherHandle),
          ),
          index = 1,
          count = 2,
          onClick = onDeleteDay,
          destructive = true,
        )
      }
    }
  }
}

@Composable
private fun ActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String?,
  index: Int,
  count: Int,
  onClick: () -> Unit,
  destructive: Boolean = false,
) {
  val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
  Surface(
    onClick = onClick,
    shape = groupedItemShape(index, count),
    color = GroupedListDefaults.containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    ListItem(
      headlineContent = { Text(title, fontWeight = FontWeight.Bold, color = tint) },
      supportingContent = subtitle?.let { { Text(it) } },
      leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
  }
}

/** Diàleg per triar la persona a qui es mou una entrada. */
@Composable
fun MoveEntryDialog(people: List<PersonEntity>, onDismiss: () -> Unit, onSelect: (PersonEntity) -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.move_entry_title)) },
    text = {
      Column {
        people.forEach { person ->
          Row(
            modifier = Modifier.fillMaxWidth().clickable { onSelect(person) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Avatar(initialsOf(person.name), person.shape, person.color, size = 40.dp, locked = person.isProtected)
            Text(person.name, style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
  )
}

@Composable
fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(R.string.delete_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
  )
}

package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.DashedActionRow
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.theme.AppColors
import com.alorma.contactnotes.designsystem.theme.SheetTitleStyle
import com.alorma.contactnotes.ui.people.initialsOf

private const val FilterThreshold = 8

/** Full per triar persona: les protegides només mostren el nom i el cadenat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonPickerSheet(
  people: List<PersonEntity>,
  selectedId: Long?,
  onSelect: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  onDismiss: () -> Unit,
) {
  AppColors {
    ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      PersonPickerContent(people = people, selectedId = selectedId, onSelect = onSelect, onNewPerson = onNewPerson)
    }
  }
}

/** Contingut del full de triar persona; el `ModalBottomSheet` és una finestra a part. */
@Composable
fun PersonPickerContent(
  people: List<PersonEntity>,
  selectedId: Long?,
  onSelect: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val visible =
    remember(people, query) {
      if (query.isBlank()) people else people.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .imePadding()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.pick_person), style = SheetTitleStyle, modifier = Modifier.padding(start = 4.dp))
    if (people.size > FilterThreshold) {
      val container = MaterialTheme.colorScheme.surfaceContainerHigh
      TextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_by_name)) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
      )
    }
    LazyColumn(
      modifier = Modifier.weight(1f, fill = false),
      verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
    ) {
      itemsIndexed(visible, key = { _, person -> person.id }, contentType = { _, _ -> "person" }) { index, person ->
        Surface(
          onClick = { onSelect(person) },
          shape = groupedItemShape(index, visible.size),
          color = GroupedListDefaults.containerColor,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Avatar(initialsOf(person.name), person.shape, person.color, size = 44.dp, locked = person.isProtected)
            Text(
              text = person.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            if (person.isProtected) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (person.id == selectedId) {
              Icon(Icons.Rounded.Check, contentDescription = null)
            }
          }
        }
      }
    }
    DashedActionRow(
      icon = Icons.Rounded.PersonAdd,
      label = stringResource(R.string.add_person),
      onClick = onNewPerson,
      modifier = Modifier.padding(bottom = 16.dp),
    )
  }
}

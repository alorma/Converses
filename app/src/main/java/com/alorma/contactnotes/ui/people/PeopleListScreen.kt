package com.alorma.contactnotes.ui.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ConversesFilterChip
import com.alorma.contactnotes.designsystem.components.ConversesFloatingToolbar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ToolbarDestination
import com.alorma.contactnotes.designsystem.components.groupedItems
import com.alorma.contactnotes.designsystem.theme.ListTitleStyle
import org.koin.compose.viewmodel.koinViewModel

private val ScreenMargin = 16.dp

@Composable
fun PeopleListRoute(
  onPersonClick: (PersonEntity) -> Unit,
  onQuickNote: () -> Unit,
  onOpenSettings: () -> Unit,
  onNewPerson: () -> Unit,
  viewModel: PeopleListViewModel = koinViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  PeopleListScreen(
    state = state,
    onFilterSelected = viewModel::onFilterSelected,
    onPersonClick = onPersonClick,
    onQuickNote = onQuickNote,
    onOpenSettings = onOpenSettings,
    onNewPerson = onNewPerson,
  )
}

@Composable
fun PeopleListScreen(
  state: PeopleListUiState,
  onFilterSelected: (ChannelFilter) -> Unit,
  onPersonClick: (PersonEntity) -> Unit,
  onQuickNote: () -> Unit,
  onOpenSettings: () -> Unit,
  onNewPerson: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenMargin, end = ScreenMargin, bottom = 136.dp),
        verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
      ) {
        item(key = "header") {
          Header(total = state.total, isLoading = state.isLoading, onNewPerson = onNewPerson, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }
        item(key = "filters") {
          FilterRow(selected = state.filter, onSelected = onFilterSelected)
        }
        if (!state.isLoading && state.people.isEmpty()) {
          item(key = "empty") {
            Text(
              text = stringResource(R.string.people_empty),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(24.dp),
            )
          }
        }
        groupedItems(state.people, key = { it.person.id }) { item ->
          PersonRow(item, onClick = { onPersonClick(item.person) })
        }
      }

      ConversesFloatingToolbar(
        fabIcon = Icons.Rounded.EditNote,
        fabContentDescription = stringResource(R.string.quick_note),
        onFabClick = onQuickNote,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = ScreenMargin, vertical = 16.dp),
      ) {
        ToolbarDestination(
          icon = Icons.Rounded.Groups,
          contentDescription = stringResource(R.string.people_title),
          selected = true,
          onClick = {},
        )
        ToolbarDestination(
          icon = Icons.Rounded.Settings,
          contentDescription = stringResource(R.string.settings_title),
          selected = false,
          onClick = onOpenSettings,
        )
      }
    }
  }
}

@Composable
private fun Header(total: Int, isLoading: Boolean, onNewPerson: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 20.dp),
    verticalAlignment = Alignment.Top,
  ) {
  Column(modifier = Modifier.weight(1f)) {
    Text(text = stringResource(R.string.people_title), style = ListTitleStyle)
    if (!isLoading) {
      Text(
        text = pluralStringResource(R.plurals.people_count, total, total),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
  FilledTonalIconButton(onClick = onNewPerson) {
    Icon(Icons.Rounded.PersonAdd, contentDescription = stringResource(R.string.add_person))
  }
  }
}

@Composable
private fun FilterRow(selected: ChannelFilter, onSelected: (ChannelFilter) -> Unit) {
  LazyRow(
    contentPadding = PaddingValues(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(ChannelFilter.entries, key = { it.name }) { filter ->
      ConversesFilterChip(
        label = stringResource(filter.label()),
        selected = filter == selected,
        onClick = { onSelected(filter) },
      )
    }
  }
}

@Composable
private fun PersonRow(item: PersonListItem, onClick: () -> Unit) {
  val person = item.person
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Avatar(
      initials = initialsOf(person.name),
      shape = person.avatarShape,
      color = person.avatarColor,
      size = 56.dp,
      locked = person.isProtected,
    )
    Column(modifier = Modifier.weight(1f)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = person.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (person.isProtected) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp).size(16.dp),
          )
        }
        Spacer(Modifier.weight(1f))
        item.lastEntry?.let { entry ->
          Text(
            text = formatLastActivity(entry.occurredAt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
          )
        }
      }
      if (person.isProtected) {
        Spacer(Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.protected_hint),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        val channels = item.channels.map { it.type }.distinct()
        if (channels.isNotEmpty()) {
          Text(
            text = channels.map { stringResource(it.label()).uppercase() }.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        item.lastEntry?.let { entry ->
          Text(
            text = entry.text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

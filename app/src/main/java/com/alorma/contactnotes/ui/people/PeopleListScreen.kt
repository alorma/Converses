package com.alorma.contactnotes.ui.people

import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.data.db.color
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
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
import androidx.compose.ui.graphics.Color
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
import com.alorma.contactnotes.designsystem.components.ConversesConnectedFilterRow
import com.alorma.contactnotes.designsystem.components.DashedActionRow
import com.alorma.contactnotes.designsystem.components.EmptyState
import com.alorma.contactnotes.designsystem.components.ConversesFloatingToolbar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ToolbarDestination
import com.alorma.contactnotes.designsystem.components.groupedItems
import com.alorma.contactnotes.designsystem.theme.ListTitleStyle
import com.alorma.contactnotes.ui.person.channelName
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenMargin = 16.dp

@Composable
fun PeopleListRoute(
  archived: Boolean,
  onPersonClick: (PersonEntity) -> Unit,
  onQuickNote: () -> Unit,
  onOpenSettings: () -> Unit,
  onNewPerson: () -> Unit,
  onToggleArchivedList: () -> Unit,
  viewModel: PeopleListViewModel = koinViewModel(parameters = { parametersOf(archived) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  PeopleListScreen(
    state = state,
    archived = archived,
    onToggleArchivedList = onToggleArchivedList,
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
  archived: Boolean,
  onToggleArchivedList: () -> Unit,
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
          Header(
            archived = archived,
            total = state.total,
            isLoading = state.isLoading,
            onNewPerson = onNewPerson,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
          )
        }
        if (!archived) {
          item(key = "filters") {
            FilterRow(selected = state.filter, onSelected = onFilterSelected)
          }
        }
        if (!state.isLoading && state.people.isEmpty()) {
          item(key = "empty") {
            val noPeopleAtAll = !archived && state.filter == ChannelFilter.All
            EmptyState(
              icon = if (archived) Icons.Rounded.Archive else Icons.Rounded.Groups,
              title = stringResource(
                when {
                  archived -> R.string.archived_empty
                  noPeopleAtAll -> R.string.people_empty
                  else -> R.string.people_empty_filter
                },
              ),
              actionLabel = if (noPeopleAtAll) stringResource(R.string.add_first_person) else null,
              onAction = if (noPeopleAtAll) onNewPerson else null,
            )
          }
        }
        groupedItems(state.people, key = { it.person.id }) { item ->
          PersonRow(item, onClick = { onPersonClick(item.person) })
        }
        if (!state.isLoading && !archived && state.people.isNotEmpty()) {
          item(key = "add_person") {
            DashedActionRow(
              icon = Icons.Rounded.PersonAdd,
              label = stringResource(R.string.add_person),
              onClick = onNewPerson,
              modifier = Modifier.padding(top = 12.dp),
            )
          }
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
          selected = !archived,
          onClick = { if (archived) onToggleArchivedList() },
        )
        ToolbarDestination(
          icon = Icons.Rounded.Archive,
          contentDescription = stringResource(R.string.filter_archived),
          selected = archived,
          onClick = { if (!archived) onToggleArchivedList() },
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
private fun Header(archived: Boolean, total: Int, isLoading: Boolean, onNewPerson: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = if (archived) 14.dp else 0.dp),
    verticalAlignment = Alignment.Top,
  ) {
  Column(modifier = Modifier.weight(1f)) {
    Text(text = stringResource(if (archived) R.string.filter_archived else R.string.people_title), style = ListTitleStyle)
    if (!isLoading) {
      Text(
        text = pluralStringResource(R.plurals.people_count, total, total),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
  if (!archived) {
    FilledTonalIconButton(onClick = onNewPerson) {
      Icon(Icons.Rounded.PersonAdd, contentDescription = stringResource(R.string.add_person))
    }
  }
  }
}

@Composable
private fun FilterRow(selected: ChannelFilter, onSelected: (ChannelFilter) -> Unit) {
  ConversesConnectedFilterRow(
    options = ChannelFilter.entries.map { it to stringResource(it.label()) },
    selected = selected,
    onSelect = onSelected,
    modifier = Modifier.padding(vertical = 12.dp),
  )
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
      shape = person.shape,
      color = person.color,
      size = 56.dp,
      locked = person.isProtected,
    )
    Column(modifier = Modifier.weight(1f)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
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
        }
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
        val channels = item.channels.map { it.type }.distinct().sorted()
        if (channels.isNotEmpty()) {
          Text(
            text = channels.map { channelName(it).uppercase() }.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = item.lastEntry?.text ?: stringResource(R.string.no_entries_yet),
          style = MaterialTheme.typography.bodyLarge,
          color = if (item.lastEntry == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

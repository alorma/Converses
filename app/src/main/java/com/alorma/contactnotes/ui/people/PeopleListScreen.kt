package com.alorma.contactnotes.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.FilterChip
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
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ConversesFloatingToolbar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ToolbarDestination
import com.alorma.contactnotes.designsystem.components.groupedItems
import com.alorma.contactnotes.designsystem.theme.ListTitleStyle

@Composable
fun PeopleListRoute(
  onQuickNote: () -> Unit,
  viewModel: PeopleListViewModel = koinViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  PeopleListScreen(
    state = state,
    onFilterSelected = viewModel::onFilterSelected,
    onQuickNote = onQuickNote,
  )
}

@Composable
fun PeopleListScreen(
  state: PeopleListUiState,
  onFilterSelected: (ChannelFilter) -> Unit,
  onQuickNote: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
      ) {
        item(key = "header") {
          Header(total = state.total, modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
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
          PersonRow(item)
        }
      }

      ConversesFloatingToolbar(
        fabIcon = Icons.Rounded.EditNote,
        fabContentDescription = stringResource(R.string.quick_note),
        onFabClick = onQuickNote,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(bottom = 16.dp),
      ) {
        ToolbarDestination(
          icon = Icons.Rounded.Groups,
          contentDescription = stringResource(R.string.people_title),
          selected = true,
          onClick = {},
        )
      }
    }
  }
}

@Composable
private fun Header(total: Int, modifier: Modifier = Modifier) {
  Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
    Text(text = stringResource(R.string.people_title), style = ListTitleStyle)
    Text(
      text = pluralStringResource(R.plurals.people_count, total, total),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun FilterRow(selected: ChannelFilter, onSelected: (ChannelFilter) -> Unit) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(ChannelFilter.entries, key = { it.name }) { filter ->
      FilterChip(
        selected = filter == selected,
        onClick = { onSelected(filter) },
        label = { Text(stringResource(filter.label())) },
        leadingIcon = if (filter == selected) {
          { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
          null
        },
      )
    }
  }
}

@Composable
private fun PersonRow(item: PersonListItem) {
  val person = item.person
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Avatar(
      initials = initialsOf(person.name),
      shape = person.avatarShape,
      color = person.avatarColor,
      size = 56.dp,
    )
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = person.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        item.lastEntry?.let { entry ->
          Text(
            text = formatLastActivity(entry.occurredAt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
          )
        }
      }
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

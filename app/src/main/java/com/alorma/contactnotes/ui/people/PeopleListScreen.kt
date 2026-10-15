package com.alorma.contactnotes.ui.people

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.BetaBadge
import com.alorma.contactnotes.designsystem.components.DashedActionRow
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.WelcomeEmptyState
import com.alorma.contactnotes.designsystem.components.groupedItems
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.designsystem.theme.ListTitleStyle
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.initialsOf
import com.alorma.contactnotes.ui.navigation.LocalFloatingBarPadding
import com.alorma.contactnotes.ui.person.channelName
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenMargin = 16.dp

@Composable
fun PeopleListRoute(
  archived: Boolean,
  onPersonClick: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  viewModel: PeopleListViewModel = koinViewModel(parameters = { parametersOf(archived) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  PeopleListScreen(
    state = state,
    archived = archived,
    onFilterToggled = viewModel::onFilterToggled,
    onPersonClick = onPersonClick,
    onNewPerson = onNewPerson,
  )
}

@Composable
fun PeopleListScreen(
  state: PeopleListUiState,
  archived: Boolean,
  onFilterToggled: (ChannelType) -> Unit,
  onPersonClick: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
) {
  val listState = rememberLazyListState()
  // Drawn once the rows are laid out (or there is nothing to show), not just when the data arrives
  ReportDrawnWhen { !state.isLoading && (state.people.isEmpty() || listState.layoutInfo.visibleItemsInfo.isNotEmpty()) }
  // En canviar els xips la llista torna a dalt (no a la primera composició, perquè no perdi la posició en tornar del detall)
  var lastFilter by remember { mutableStateOf(state.filter) }
  LaunchedEffect(state.filter) {
    if (state.filter != lastFilter) {
      lastFilter = state.filter
      listState.scrollToItem(0)
    }
  }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    topBar = {
      // Capçalera i filtres fixos a dalt, amb fons opac perquè la llista passi per sota, també l'àrea de la barra d'estat
      Column(
        modifier =
          Modifier
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
      ) {
        Header(
          archived = archived,
          total = state.total,
          isLoading = state.isLoading,
          onNewPerson = onNewPerson,
        )
        if (!archived) {
          FilterRow(selected = state.filter, onToggle = onFilterToggled)
        }
      }
    },
  ) { padding ->
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding =
        PaddingValues(
          start = ScreenMargin,
          end = ScreenMargin,
          top = padding.calculateTopPadding(),
          bottom = LocalFloatingBarPadding.current.calculateBottomPadding() + padding.calculateBottomPadding(),
        ),
      verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
    ) {
      if (!state.isLoading && state.people.isEmpty()) {
        val noPeopleAtAll = !archived && state.filter.isEmpty()
        item(key = "empty") {
          if (noPeopleAtAll) {
            WelcomeEmptyState(
              icon = Icons.Rounded.Forum,
              title = stringResource(R.string.people_empty),
              description = stringResource(R.string.people_empty_description),
              actionLabel = stringResource(R.string.add_person),
              actionIcon = Icons.Rounded.PersonAdd,
              onAction = onNewPerson,
              modifier = Modifier.fillParentMaxSize(),
            )
          } else {
            WelcomeEmptyState(
              icon = if (archived) Icons.Rounded.Archive else Icons.Rounded.Groups,
              title = stringResource(if (archived) R.string.archived_empty else R.string.people_empty_filter),
              description =
                stringResource(
                  if (archived) R.string.archived_empty_description else R.string.people_empty_filter_description,
                ),
              modifier = Modifier.fillParentMaxSize(),
            )
          }
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
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(
  archived: Boolean,
  total: Int,
  isLoading: Boolean,
  onNewPerson: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = if (archived) 14.dp else 0.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      // Amb lletra gran el badge baixa a la línia següent en comptes de partir el títol
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = stringResource(if (archived) R.string.filter_archived else R.string.people_title),
          style = ListTitleStyle,
          maxLines = 1,
          softWrap = false,
        )
        if (!archived) {
          BetaBadge(
            text = stringResource(R.string.beta_badge),
            contentDescription = stringResource(R.string.beta_description),
            modifier = Modifier.padding(top = 7.dp),
          )
        }
      }
      if (!isLoading) {
        Text(
          text =
            if (total == 0 &&
              !archived
            ) {
              stringResource(R.string.people_count_none)
            } else {
              pluralStringResource(R.plurals.people_count, total, total)
            },
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
private fun FilterRow(
  selected: Set<ChannelType>,
  onToggle: (ChannelType) -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val haptics = rememberConversesHaptics()
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(top = 12.dp, bottom = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ChannelType.entries.forEach { group ->
      val isSelected = group in selected
      FilterChip(
        selected = isSelected,
        onClick = {
          haptics.segmentTick()
          onToggle(group)
        },
        label = {
          Text(
            text = stringResource(group.label()),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
          )
        },
        leadingIcon =
          if (isSelected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
          } else {
            null
          },
        shape = RoundedCornerShape(8.dp),
        colors =
          FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = scheme.onSurfaceVariant,
            selectedContainerColor = scheme.primaryContainer,
            selectedLabelColor = scheme.onPrimaryContainer,
            selectedLeadingIconColor = scheme.onPrimaryContainer,
          ),
        border =
          FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = scheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp,
          ),
      )
    }
  }
}

@Composable
private fun PersonRow(
  item: PersonListItem,
  onClick: () -> Unit,
) {
  val person = item.person
  Row(
    modifier =
      Modifier
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
        val channels = item.channels.distinct().sorted()
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

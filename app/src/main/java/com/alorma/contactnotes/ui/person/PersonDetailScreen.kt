package com.alorma.contactnotes.ui.person

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ConversesFilterChip
import com.alorma.contactnotes.designsystem.components.GroupedList
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.TimelineMarker
import com.alorma.contactnotes.designsystem.components.TimelineRow
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import com.alorma.contactnotes.designsystem.theme.SectionTitleStyle
import com.alorma.contactnotes.ui.people.initialsOf
import com.alorma.contactnotes.ui.people.label
import java.time.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenMargin = 16.dp

private sealed interface TimelineItem {
  data class Day(val date: LocalDate) : TimelineItem
  data class Entry(val entry: EntryEntity) : TimelineItem
}

private fun List<EntryEntity>.toTimelineItems(): List<TimelineItem> = buildList {
  var currentDay: LocalDate? = null
  for (entry in this@toTimelineItems) {
    val day = entryDate(entry.occurredAt)
    if (day != currentDay) {
      currentDay = day
      add(TimelineItem.Day(day))
    }
    add(TimelineItem.Entry(entry))
  }
}

@Composable
fun PersonDetailRoute(
  personId: Long,
  onBack: () -> Unit,
  viewModel: PersonDetailViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  val window = LocalActivity.current?.window
  val secure = state.person?.isProtected == true
  DisposableEffect(secure, window) {
    if (secure) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
  }

  PersonDetailScreen(state = state, onBack = onBack, onKindToggled = viewModel::onKindToggled)
}

@Composable
fun PersonDetailScreen(
  state: PersonDetailUiState,
  onBack: () -> Unit,
  onKindToggled: (com.alorma.contactnotes.data.db.EntryKind) -> Unit,
) {
  val person = state.person
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    if (person == null) return@Surface
    val items = remember(state.entries) { state.entries.toTimelineItems() }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = ScreenMargin, end = ScreenMargin, bottom = 96.dp),
    ) {
      item(key = "back") {
        IconButton(onClick = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
        }
      }
      item(key = "header") { Header(person, state) }
      if (state.channels.isNotEmpty()) {
        item(key = "channels") { ChannelRow(state.channels) }
      }
      if (state.facts.isNotEmpty()) {
        item(key = "facts") { PinnedFacts(state.facts) }
      }
      item(key = "timeline-header") {
        TimelineHeader(count = state.entries.size)
      }
      item(key = "kind-filters") {
        LazyRow(
          contentPadding = PaddingValues(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(com.alorma.contactnotes.data.db.EntryKind.entries, key = { it.name }) { kind ->
            ConversesFilterChip(
              label = stringResource(kind.label()),
              selected = kind in state.selectedKinds,
              onClick = { onKindToggled(kind) },
              icon = kind.icon(),
              tonal = true,
            )
          }
        }
      }
      if (items.isEmpty()) {
        item(key = "empty") {
          Text(
            text = stringResource(R.string.timeline_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
          )
        }
      }
      items(items.size, key = { index ->
        when (val item = items[index]) {
          is TimelineItem.Day -> "day-${item.date}"
          is TimelineItem.Entry -> "entry-${item.entry.id}"
        }
      }) { index ->
        val isFirst = index == 0
        val isLast = index == items.lastIndex
        when (val item = items[index]) {
          is TimelineItem.Day -> TimelineRow(isFirst = isFirst, isLast = isLast) {
            Text(
              text = dayLabel(item.date),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
          }
          is TimelineItem.Entry -> TimelineRow(
            isFirst = isFirst,
            isLast = isLast,
            marker = { TimelineMarker(icon = item.entry.kind.icon(), style = item.entry.kind.markerStyle()) },
          ) {
            EntryCard(item.entry)
          }
        }
      }
    }
  }
}

@Composable
private fun Header(person: PersonEntity, state: PersonDetailUiState) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = person.name, style = PersonNameStyle)
      val since = stringResource(R.string.since, formatMonthYear(person.createdAt))
      Text(
        text = person.alias?.let { "@$it · $since" } ?: since,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.padding(top = 12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Counter(state.saidCount, stringResource(R.string.kind_said))
        Counter(state.heardCount, stringResource(R.string.kind_heard))
        Counter(state.totalCount, stringResource(R.string.conversations))
      }
    }
    Avatar(
      initials = initialsOf(person.name),
      shape = person.avatarShape,
      color = person.avatarColor,
      size = 80.dp,
    )
  }
}

@Composable
private fun Counter(value: Int, label: String) {
  Column {
    Text(text = value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ChannelRow(channels: List<ChannelEntity>) {
  LazyRow(
    contentPadding = PaddingValues(vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(channels, key = { it.id }) { channel ->
      AssistChip(
        onClick = {},
        label = { Text(stringResource(channel.type.label()), fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(channel.type.icon(), contentDescription = null, modifier = Modifier.width(AssistChipDefaults.IconSize)) },
        shape = MaterialTheme.shapes.large,
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = null,
      )
    }
  }
}

@Composable
private fun PinnedFacts(facts: List<PinnedFactEntity>) {
  Column(modifier = Modifier.padding(bottom = 8.dp)) {
    Row(
      modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(Icons.Rounded.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = stringResource(R.string.pinned_facts),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    GroupedList(items = facts, modifier = Modifier.fillMaxWidth()) { fact ->
      Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
          text = fact.label.uppercase(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.width(104.dp),
        )
        Text(text = fact.value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun TimelineHeader(count: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 24.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.timeline),
      style = SectionTitleStyle,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = pluralStringResource(R.plurals.entry_count, count, count),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun EntryCard(entry: EntryEntity) {
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = GroupedListDefaults.containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
      Text(
        text = listOf(
          stringResource(entry.kind.label()),
          formatTime(entry.occurredAt),
          stringResource(entry.channelType.label()),
        ).joinToString(" · ") { it.uppercase() },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(text = entry.text, style = MaterialTheme.typography.bodyLarge)
    }
  }
}

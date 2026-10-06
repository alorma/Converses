package com.alorma.contactnotes.ui.person

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ConversesChannelChip
import com.alorma.contactnotes.designsystem.components.ConversesFilterChip
import com.alorma.contactnotes.designsystem.components.ConversesMenuItem
import com.alorma.contactnotes.designsystem.components.ConversesSplitButton
import com.alorma.contactnotes.designsystem.components.EmptyState
import com.alorma.contactnotes.designsystem.components.GroupedList
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.TimelineMarker
import com.alorma.contactnotes.designsystem.components.TimelineRow
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import com.alorma.contactnotes.designsystem.theme.SectionTitleStyle
import com.alorma.contactnotes.ui.entry.NewEntrySheet
import com.alorma.contactnotes.ui.people.initialsOf
import com.alorma.contactnotes.ui.people.label
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenMargin = 16.dp
private const val UndoWindowMillis = 5_000L

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

/** Quina acció d'una entrada està oberta. */
private enum class EntryAction { None, Menu, Edit, Move, ConfirmDeleteDay }

@Composable
fun PersonDetailRoute(
  personId: Long,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  viewModel: PersonDetailViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val otherPeople by viewModel.otherPeople.collectAsStateWithLifecycle()

  val secureWindow: SecureWindow = koinInject()
  val secure = state.person?.isProtected == true
  DisposableEffect(secure) {
    if (secure) secureWindow.acquire()
    onDispose { if (secure) secureWindow.release() }
  }

  val person = state.person ?: return
  var showNewEntry by rememberSaveable { mutableStateOf(false) }
  var menuExpanded by remember { mutableStateOf(false) }
  var actionEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
  var action by rememberSaveable { mutableStateOf(EntryAction.None) }
  val actionEntry = state.entries.firstOrNull { it.id == actionEntryId }
  val clipboard = LocalClipboard.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val deletedMessage = stringResource(R.string.entry_deleted)
  val undoLabel = stringResource(R.string.undo)
  var dayCount by remember { mutableIntStateOf(0) }
  LaunchedEffect(actionEntry?.id) { actionEntry?.let { dayCount = viewModel.countDay(it) } }

  fun closeAction() {
    action = EntryAction.None
    actionEntryId = null
  }

  PersonDetailScreen(
    state = state,
    menuExpanded = menuExpanded,
    onMenuExpandedChange = { menuExpanded = it },
    onBack = onBack,
    onKindToggled = viewModel::onKindToggled,
    onNewEntry = { showNewEntry = true },
    onEntryMenu = { entry ->
      actionEntryId = entry.id
      action = EntryAction.Menu
    },
    onEditPerson = {
      menuExpanded = false
      onEdit()
    },
    onExport = {
      menuExpanded = false
      viewModel.export()
    },
    onToggleArchived = {
      menuExpanded = false
      viewModel.setArchived(!person.archived) { if (!person.archived) onBack() }
    },
    onChannelOpen = { channel ->
      channelLink(channel.type, channel.handle)?.let { uri ->
        try {
          context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
          Unit
        }
      }
    },
    onChannelCopy = { channel ->
      scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(person.name, channel.handle.orEmpty()))) }
    },
    snackbarHostState = snackbarHostState,
  )

  if (showNewEntry) {
    NewEntrySheet(
      person = person,
      channels = state.channels.map { it.type },
      onDismiss = { showNewEntry = false },
      onSave = { kind, channel, text, at ->
        viewModel.addEntry(kind, channel, text, at)
        showNewEntry = false
      },
    )
  }

  if (actionEntry != null) {
    when (action) {
      EntryAction.Menu -> EntryActionsSheet(
        entry = actionEntry,
        dayCount = dayCount,
        onDismiss = ::closeAction,
        onEdit = { action = EntryAction.Edit },
        onCopy = {
          scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(person.name, actionEntry.text))) }
          closeAction()
        },
        onMove = { action = EntryAction.Move },
        onDelete = {
          val deleted = actionEntry
          viewModel.deleteEntry(deleted)
          closeAction()
          scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val timeout = launch {
              delay(UndoWindowMillis)
              snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
              message = deletedMessage,
              actionLabel = undoLabel,
              duration = SnackbarDuration.Indefinite,
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(deleted)
          }
        },
        onDeleteDay = { action = EntryAction.ConfirmDeleteDay },
      )
      EntryAction.Edit -> NewEntrySheet(
        person = person,
        channels = state.channels.map { it.type },
        initial = actionEntry,
        onDismiss = ::closeAction,
        onSave = { kind, channel, text, at ->
          viewModel.updateEntry(actionEntry, kind, channel, text, at)
          closeAction()
        },
      )
      EntryAction.Move -> MoveEntryDialog(
        people = otherPeople,
        onDismiss = ::closeAction,
        onSelect = {
          viewModel.moveEntry(actionEntry, it.id)
          closeAction()
        },
      )
      EntryAction.ConfirmDeleteDay -> ConfirmDeleteDialog(
        title = stringResource(R.string.delete_day_title),
        text = pluralStringResource(
          R.plurals.entry_delete_day_summary,
          dayCount,
          dayCount,
          stringResource(actionEntry.channelType.label()),
        ),
        onDismiss = ::closeAction,
        onConfirm = {
          viewModel.deleteDay(actionEntry)
          closeAction()
        },
      )
      EntryAction.None -> Unit
    }
  }
}

@Composable
fun PersonDetailScreen(
  state: PersonDetailUiState,
  menuExpanded: Boolean,
  onMenuExpandedChange: (Boolean) -> Unit,
  onBack: () -> Unit,
  onKindToggled: (EntryKind) -> Unit,
  onNewEntry: () -> Unit,
  onEntryMenu: (EntryEntity) -> Unit,
  onEditPerson: () -> Unit,
  onExport: () -> Unit,
  onToggleArchived: () -> Unit,
  onChannelOpen: (ChannelEntity) -> Unit,
  onChannelCopy: (ChannelEntity) -> Unit,
  snackbarHostState: SnackbarHostState,
) {
  val person = state.person
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    if (person == null) return@Surface
    val items = remember(state.entries) { state.entries.toTimelineItems() }

    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = ScreenMargin,
          end = ScreenMargin,
          bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
      ) {
        item(key = "back") {
          IconButton(onClick = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        }
        item(key = "header") { Header(person, state) }
        if (state.channels.isNotEmpty()) {
          item(key = "channels") { ChannelRow(state.channels, onChannelOpen, onChannelCopy) }
        }
        if (state.facts.isNotEmpty()) {
          item(key = "facts") { PinnedFacts(state.facts) }
        }
        item(key = "timeline-header") { TimelineHeader(count = state.entries.size) }
        item(key = "kind-filters") {
          LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(EntryKind.entries, key = { it.name }) { kind ->
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
            val noEntries = state.saidCount + state.heardCount + state.happenedCount == 0
            val kindsLabel = EntryKind.entries
              .filter { it in state.selectedKinds }
              .map { stringResource(it.label()) }
              .joinToString(" · ")
            EmptyState(
              icon = Icons.Rounded.ChatBubbleOutline,
              title = if (noEntries || kindsLabel.isEmpty()) {
                stringResource(R.string.timeline_empty)
              } else {
                stringResource(R.string.timeline_filtered_empty, kindsLabel)
              },
              actionLabel = if (noEntries) stringResource(R.string.note_button) else null,
              onAction = if (noEntries) onNewEntry else null,
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
              EntryCard(item.entry, onMenu = { onEntryMenu(item.entry) })
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(start = 16.dp, end = 16.dp, bottom = 88.dp),
      )

      ConversesSplitButton(
        label = stringResource(R.string.note_button),
        icon = Icons.Rounded.Edit,
        onClick = onNewEntry,
        menuExpanded = menuExpanded,
        onMenuExpandedChange = onMenuExpandedChange,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(16.dp),
      ) {
        ConversesMenuItem(
          text = stringResource(R.string.edit_person),
          icon = Icons.Rounded.Edit,
          onClick = onEditPerson,
        )
        ConversesMenuItem(
          text = stringResource(R.string.menu_export),
          icon = Icons.Rounded.IosShare,
          onClick = onExport,
        )
        ConversesMenuItem(
          text = stringResource(if (person.archived) R.string.menu_unarchive else R.string.menu_archive),
          icon = if (person.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
          onClick = onToggleArchived,
        )
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
        Counter(state.happenedCount, stringResource(R.string.kind_happened))
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
private fun ChannelRow(
  channels: List<ChannelEntity>,
  onOpen: (ChannelEntity) -> Unit,
  onCopy: (ChannelEntity) -> Unit,
) {
  LazyRow(
    contentPadding = PaddingValues(vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(channels, key = { it.id }) { channel ->
      val hasLink = channelLink(channel.type, channel.handle) != null
      val hasHandle = !channel.handle.isNullOrBlank()
      ConversesChannelChip(
        label = stringResource(channel.type.label()),
        icon = channel.type.icon(),
        onClick = if (hasLink) ({ onOpen(channel) }) else null,
        onLongClick = if (hasHandle) ({ onCopy(channel) }) else null,
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
private fun EntryCard(entry: EntryEntity, onMenu: () -> Unit) {
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = GroupedListDefaults.containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(verticalAlignment = Alignment.Top) {
      Column(modifier = Modifier.weight(1f).padding(start = 16.dp, top = 14.dp, bottom = 14.dp)) {
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
      IconButton(onClick = onMenu) {
        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.entry_actions))
      }
    }
  }
}

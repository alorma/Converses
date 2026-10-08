package com.alorma.contactnotes.ui.person

import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.data.db.color
import android.content.ClipData
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.alorma.contactnotes.designsystem.components.DashedHintRow
import com.alorma.contactnotes.designsystem.components.DashedPill
import com.alorma.contactnotes.designsystem.components.EmptyState
import com.alorma.contactnotes.designsystem.components.GroupedList
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.TimelineRow
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.designsystem.theme.SectionTitleStyle
import com.alorma.contactnotes.ui.edit.EditFocus
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
  onEdit: (EditFocus?) -> Unit,
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
  // Quan s'obre des de l'entrada fantasma, ve amb el «Qui» triat i el focus al text
  var newEntryKind by rememberSaveable { mutableStateOf(EntryKind.SAID) }
  var newEntryFocus by rememberSaveable { mutableStateOf(false) }
  var menuExpanded by remember { mutableStateOf(false) }
  var actionEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
  var action by rememberSaveable { mutableStateOf(EntryAction.None) }
  val actionEntry = state.entries.firstOrNull { it.id == actionEntryId }
  val clipboard = LocalClipboard.current
  val haptics = rememberConversesHaptics()
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val deletedMessage = stringResource(R.string.entry_deleted)
  val undoLabel = stringResource(R.string.undo)
  var dayCount by remember { mutableIntStateOf(0) }
  LaunchedEffect(actionEntry?.id) { actionEntry?.let { dayCount = viewModel.countDay(it) } }

  fun closeAction() {
    action = EntryAction.None
    actionEntryId = null
  }

  PersonTheme(person.color) {
    PersonDetailScreen(
      state = state,
      menuExpanded = menuExpanded,
      onMenuExpandedChange = { menuExpanded = it },
      onBack = onBack,
      onKindToggled = viewModel::onKindToggled,
      onNewEntry = {
        newEntryKind = EntryKind.SAID
        newEntryFocus = false
        showNewEntry = true
      },
      onNewEntryAs = { kind ->
        newEntryKind = kind
        newEntryFocus = true
        showNewEntry = true
      },
      onEditFocus = { onEdit(it) },
      onEntryMenu = { entry ->
        actionEntryId = entry.id
        action = EntryAction.Menu
      },
      onEditPerson = { onEdit(null) },
      onExport = {
        menuExpanded = false
        viewModel.export()
      },
      onToggleArchived = {
        menuExpanded = false
        viewModel.setArchived(!person.archived) { if (!person.archived) onBack() }
      },
      snackbarHostState = snackbarHostState,
    )

    if (showNewEntry) {
      NewEntrySheet(
        person = person,
        channels = state.channels.map { it.type },
        initialKind = newEntryKind,
        focusText = newEntryFocus,
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
          firstName = firstName(person.name),
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
            haptics.confirm()
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
            channelName(actionEntry.channelType),
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
}

@Composable
fun PersonDetailScreen(
  state: PersonDetailUiState,
  menuExpanded: Boolean,
  onMenuExpandedChange: (Boolean) -> Unit,
  onBack: () -> Unit,
  onKindToggled: (EntryKind) -> Unit,
  onNewEntry: () -> Unit,
  onNewEntryAs: (EntryKind) -> Unit,
  onEditFocus: (EditFocus) -> Unit,
  onEntryMenu: (EntryEntity) -> Unit,
  onEditPerson: () -> Unit,
  onExport: () -> Unit,
  onToggleArchived: () -> Unit,
  snackbarHostState: SnackbarHostState,
) {
  val person = state.person
  Scaffold(
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = ScreenMargin),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Spacer(Modifier.weight(1f))
        if (person != null) {
          IconButton(onClick = onEditPerson) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_person))
          }
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 4.dp)) },
    floatingActionButton = {
      if (person != null) {
        ConversesSplitButton(
          label = stringResource(R.string.note_button),
          icon = Icons.Rounded.EditNote,
          onClick = onNewEntry,
          menuExpanded = menuExpanded,
          onMenuExpandedChange = onMenuExpandedChange,
          menuContentDescription = stringResource(R.string.more_options),
        ) {
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
    },
  ) { padding ->
    if (person == null) return@Scaffold
    val items = remember(state.entries) { state.entries.toTimelineItems() }
    val firstName = remember(person.name) { firstName(person.name) }
    val noEntries = EntryKind.entries.sumOf { state.countOf(it) } == 0

    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = ScreenMargin,
          end = ScreenMargin,
          top = padding.calculateTopPadding(),
          bottom = 96.dp + padding.calculateBottomPadding(),
        ),
      ) {
        item(key = "header") { Header(person) }
        item(key = "channels") {
          if (state.channels.isNotEmpty()) {
            ChannelRow(state.channels)
          } else {
            DashedPill(
              label = stringResource(R.string.empty_channels_cta),
              onClick = { onEditFocus(EditFocus.CHANNELS) },
              modifier = Modifier.padding(vertical = 16.dp),
            )
          }
        }
        item(key = "facts") {
          PinnedFacts(state.facts, onAddFirst = { onEditFocus(EditFocus.NEW_FACT) })
        }
        item(key = "timeline-header") {
          SectionHeader(
            title = stringResource(R.string.timeline),
            topPadding = 24.dp,
            trailing = if (noEntries) {
              null
            } else {
              pluralStringResource(R.plurals.entry_count, state.entries.size, state.entries.size)
            },
          )
        }
        if (!noEntries) item(key = "kind-filters") {
          LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(EntryKind.entries.filter { state.countOf(it) > 0 }, key = { it.name }) { kind ->
              ConversesFilterChip(
                label = whoFilterLabel(kind, firstName),
                selected = kind in state.selectedKinds,
                onClick = { onKindToggled(kind) },
                icon = kind.whoIcon(),
                tonal = true,
                leadingContent = if (kind == EntryKind.HEARD) ({ PersonInitialAvatar(person, size = 22.dp) }) else null,
              )
            }
          }
        }
        if (noEntries) {
          item(key = "ghost") {
            GhostEntry(person = person, firstName = firstName, onNewEntryAs = onNewEntryAs)
          }
        } else if (items.isEmpty()) {
          item(key = "empty") {
            val kindsLabel = EntryKind.entries
              .filter { it in state.selectedKinds }
              .map { whoFilterLabel(it, firstName) }
              .joinToString(" · ")
            EmptyState(
              icon = Icons.Rounded.ChatBubbleOutline,
              title = if (kindsLabel.isEmpty()) {
                stringResource(R.string.timeline_empty)
              } else {
                stringResource(R.string.timeline_filtered_empty, kindsLabel)
              },
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
              marker = { EntryMarker(item.entry.kind, person) },
            ) {
              EntryCard(item.entry, firstName, onMenu = { onEntryMenu(item.entry) })
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Header(person: PersonEntity) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = person.name, style = PersonNameStyle)
      person.alias?.let {
        Text(
          text = "@$it",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = stringResource(R.string.person_since, formatMonthYear(person.createdAt)),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Avatar(
      initials = initialsOf(person.name),
      shape = person.shape,
      color = person.color,
      size = 80.dp,
    )
  }
}

@Composable
private fun ChannelRow(channels: List<ChannelEntity>) {
  LazyRow(
    contentPadding = PaddingValues(vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(channels.map { it.type }.distinct().sorted(), key = { it.name }) { type ->
      ConversesChannelChip(label = channelName(type), icon = type.icon(), onClick = null, onLongClick = null)
    }
  }
}

@Composable
private fun PinnedFacts(facts: List<PinnedFactEntity>, onAddFirst: () -> Unit) {
  Column(modifier = Modifier.padding(bottom = 8.dp)) {
    SectionHeader(title = stringResource(R.string.facts_title), topPadding = 0.dp)
    if (facts.isEmpty()) {
      DashedHintRow(
        title = stringResource(R.string.empty_facts_title),
        hint = stringResource(R.string.empty_facts_hint),
        onClick = onAddFirst,
      )
      return@Column
    }
    GroupedList(items = facts, modifier = Modifier.fillMaxWidth()) { fact ->
      val stacked = LocalDensity.current.fontScale >= 1.5f
      val label: @Composable (Modifier) -> Unit = { modifier ->
        Text(
          text = fact.label.uppercase(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = modifier,
        )
      }
      if (stacked) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
          label(Modifier)
          Text(text = fact.value, style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
          label(Modifier.width(104.dp))
          Text(text = fact.value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String, topPadding: androidx.compose.ui.unit.Dp, trailing: String? = null) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = topPadding, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = title, style = SectionTitleStyle, modifier = Modifier.weight(1f))
    if (trailing != null) {
      Text(
        text = trailing,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun EntryCard(entry: EntryEntity, firstName: String, onMenu: () -> Unit) {
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = GroupedListDefaults.containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    val description = stringResource(
      R.string.entry_description,
      whoLabel(entry.kind, firstName),
      dayLabel(entryDate(entry.occurredAt)),
      formatTime(entry.occurredAt),
      channelName(entry.channelType),
      entry.text,
    )
    Row(verticalAlignment = Alignment.Top) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
          .clearAndSetSemantics { contentDescription = description },
      ) {
        Text(
          text = listOf(
            whoLabel(entry.kind, firstName),
            formatTime(entry.occurredAt),
            channelName(entry.channelType),
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

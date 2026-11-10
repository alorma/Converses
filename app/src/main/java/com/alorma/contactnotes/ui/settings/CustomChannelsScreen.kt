package com.alorma.contactnotes.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.db.CustomChannelWithCount
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.ProBadge
import com.alorma.contactnotes.designsystem.components.SettingRow
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.ui.entry.CustomChannelSheet
import com.alorma.contactnotes.ui.person.channelName
import com.alorma.contactnotes.ui.person.icon
import com.alorma.contactnotes.ui.tracking.Actions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Canals d'un grup, a la llista de [CustomChannelsViewModel]. Els grups sense canals no hi són. */
data class CustomChannelGroup(
  val group: ChannelType,
  val channels: List<CustomChannelWithCount>,
)

class CustomChannelsViewModel(
  private val repository: ConversesRepository,
  private val tracker: EventTracker,
) : ViewModel() {
  /** `null` mentre es carrega. El repositori ja els dona en l'ordre dels grups i, dins de cada grup, per nom. */
  val groups: StateFlow<List<CustomChannelGroup>?> =
    repository
      .observeCustomChannels()
      .map { list ->
        list.groupBy { it.channel.channelType }.map { (group, channels) ->
          CustomChannelGroup(group, channels)
        }
      }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  /** Les seves entrades es queden al grup. */
  fun delete(channel: CustomChannelEntity) {
    tracker.trackAction(Actions.CustomChannelDeleted())
    viewModelScope.launch { repository.deleteCustomChannel(channel) }
  }
}

@Composable
fun CustomChannelsScreen(
  onBack: () -> Unit,
  viewModel: CustomChannelsViewModel = koinViewModel(),
) {
  val groups by viewModel.groups.collectAsStateWithLifecycle()
  val haptics = rememberConversesHaptics()
  // Full 7b: «nou» (sense canal) o editar el canal amb aquest id
  var sheetOpen by rememberSaveable { mutableStateOf(false) }
  var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
  var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
  val all = groups.orEmpty().flatMap { it.channels }.map { it.channel }

  CustomChannelsContent(
    groups = groups.orEmpty(),
    onBack = onBack,
    onNew = {
      editingId = null
      sheetOpen = true
    },
    onEdit = {
      editingId = it.id
      sheetOpen = true
    },
    onDelete = { deletingId = it.id },
  )

  if (sheetOpen) {
    val editing = editingId?.let { id -> all.firstOrNull { it.id == id } }
    CustomChannelSheet(
      initialGroup = editing?.channelType ?: ChannelType.entries.first(),
      editing = editing,
      onDismiss = { sheetOpen = false },
      onSaved = {
        haptics.confirm()
        sheetOpen = false
      },
    )
  }

  deletingId?.let { id -> all.firstOrNull { it.id == id } }?.let { channel ->
    AlertDialog(
      onDismissRequest = { deletingId = null },
      title = { Text(stringResource(R.string.custom_channel_delete_title, channel.name)) },
      text = { Text(stringResource(R.string.custom_channel_delete_body, channelName(channel.channelType))) },
      confirmButton = {
        TextButton(
          onClick = {
            haptics.confirm()
            viewModel.delete(channel)
            deletingId = null
          },
        ) { Text(stringResource(R.string.custom_channel_delete), color = MaterialTheme.colorScheme.error) }
      },
      dismissButton = {
        TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.custom_channel_cancel)) }
      },
    )
  }
}

@Composable
fun CustomChannelsContent(
  groups: List<CustomChannelGroup>,
  onBack: () -> Unit,
  onNew: () -> Unit,
  onEdit: (CustomChannelEntity) -> Unit,
  onDelete: (CustomChannelEntity) -> Unit,
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    topBar = {
      ConversesTopBar(
        title = stringResource(R.string.custom_channels_title),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
        titleTrailing = {
          ProBadge(
            text = stringResource(R.string.pro_badge),
            contentDescription = stringResource(R.string.pro_description),
          )
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onNew,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.custom_channel_new_title)) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      )
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).readableWidth(),
      // Espai a baix perquè el FAB no tapi la nota del peu
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      item(key = "intro") {
        Text(
          text = stringResource(R.string.custom_channels_intro),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 8.dp),
        )
      }
      groups.forEach { (group, channels) ->
        item(key = "group-${group.name}") { GroupHeader(group) }
        itemsIndexed(channels, key = { _, it -> it.channel.id }) { index, item ->
          ChannelRow(
            item = item,
            index = index,
            count = channels.size,
            onEdit = { onEdit(item.channel) },
            onDelete = { onDelete(item.channel) },
          )
        }
      }
      item(key = "footer") {
        Text(
          text = stringResource(R.string.custom_channels_footer),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp).padding(top = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun GroupHeader(group: ChannelType) {
  Row(
    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      group.icon(),
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = channelName(group),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ChannelRow(
  item: CustomChannelWithCount,
  index: Int,
  count: Int,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }
  SettingRow(
    title = item.channel.name,
    subtitle = pluralStringResource(R.plurals.custom_channel_entries, item.entryCount, item.entryCount),
    shape = groupedItemShape(index, count),
    trailing = {
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(
            Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.custom_channel_options, item.channel.name),
          )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          // «Reanomena» i «Canvia de grup» obren el mateix full 7b, amb els camps omplerts
          DropdownMenuItem(
            text = { Text(stringResource(R.string.custom_channel_rename)) },
            onClick = {
              menuOpen = false
              onEdit()
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.custom_channel_change_group)) },
            onClick = {
              menuOpen = false
              onEdit()
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.custom_channel_delete), color = MaterialTheme.colorScheme.error) },
            onClick = {
              menuOpen = false
              onDelete()
            },
          )
        }
      }
    },
  )
}

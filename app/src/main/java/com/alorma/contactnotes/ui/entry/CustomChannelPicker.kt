package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.flags.CustomChannelsFeature
import com.alorma.contactnotes.data.flags.FeatureState
import com.alorma.contactnotes.data.flags.rememberFeatureState
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ProBadge
import com.alorma.contactnotes.designsystem.components.dashedBorder
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.shared.ChannelType
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** Canals propis per al camp «On»: l'estat del flag Pro i tots els canals, que són globals. */
@Immutable
data class CustomChannelsUi(
  val state: FeatureState = FeatureState.Hidden,
  val channels: List<CustomChannelEntity> = emptyList(),
) {
  /** El canal per defecte (el de l'última entrada) només porta el canal propi si encara es pot triar. */
  fun defaultFor(channel: EntryChannel?): EntryChannel? =
    if (state == FeatureState.Unlocked) channel else channel?.copy(customChannelId = null)
}

/** Els canals propis no són dades de cap persona: es poden carregar abans de desbloquejar una protegida. */
@Composable
fun rememberCustomChannelsUi(): CustomChannelsUi {
  val state = rememberFeatureState<CustomChannelsFeature>()
  val repository: ConversesRepository = koinInject()
  val flow = remember(repository) { repository.observeCustomChannels().map { list -> list.map { it.channel } } }
  val channels by flow.collectAsStateWithLifecycle(emptyList())
  return CustomChannelsUi(state, channels)
}

/**
 * Targeta sota els grups del camp «On» (7a, 7c). `Unlocked`: els canals del grup i «+ Nou».
 * `Locked`: «Canal propi» amb PRO; si l'entrada ja en tenia un, es continua veient triat i es pot desmarcar.
 */
@Composable
fun CustomChannelCard(
  channel: EntryChannel,
  customChannels: CustomChannelsUi,
  onChannelChange: (EntryChannel) -> Unit,
  onNewChannel: (ChannelType) -> Unit,
  onOpenPro: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = customChannels.state
  if (state == FeatureState.Hidden) return
  val unlocked = state == FeatureState.Unlocked
  val haptics = rememberConversesHaptics()
  val inGroup = customChannels.channels.filter { it.channelType == channel.group }
  val shown = if (unlocked) inGroup else inGroup.filter { it.id == channel.customChannelId }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = GroupedListDefaults.containerColor,
  ) {
    Column(
      modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text =
          if (unlocked) {
            stringResource(
              channel.group.pickTitle(),
            )
          } else {
            stringResource(R.string.custom_channels_overline)
          },
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        shown.forEach { custom ->
          val isSelected = custom.id == channel.customChannelId
          SelectableChip(
            label = custom.name,
            selected = isSelected,
            onClick = {
              if (!isSelected) haptics.segmentTick()
              onChannelChange(channel.toggle(custom))
            },
          )
        }
        if (unlocked) {
          DashedChip(onClick = { onNewChannel(channel.group) }) {
            Text(stringResource(R.string.custom_channel_new_chip))
          }
        } else {
          DashedChip(onClick = onOpenPro) {
            Text(stringResource(R.string.custom_channel_locked_chip))
            ProBadge(
              text = stringResource(R.string.pro_badge),
              contentDescription = stringResource(R.string.pro_description),
              modifier = Modifier.padding(start = 2.dp),
            )
          }
        }
      }
    }
  }
}

/** Xip amb vora discontínua: «+ Nou» o, sense Pro, «+ Canal propi PRO». */
@Composable
private fun DashedChip(
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  // Com els FilterChip: 36 dp visibles dins d'una àrea tàctil de 48 dp
  Row(
    modifier =
      Modifier
        .minimumInteractiveComponentSize()
        .height(36.dp)
        .clip(RoundedCornerShape(12.dp))
        .dashedBorder(MaterialTheme.colorScheme.outline, radius = 12.dp)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(start = 10.dp, end = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
    ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) { content() }
  }
}

private fun ChannelType.pickTitle(): Int =
  when (this) {
    ChannelType.IN_PERSON -> R.string.custom_channel_pick_in_person
    ChannelType.PHONE -> R.string.custom_channel_pick_phone
    ChannelType.CHAT -> R.string.custom_channel_pick_chat
    ChannelType.SOCIAL -> R.string.custom_channel_pick_social
    ChannelType.OTHER -> R.string.custom_channel_pick_other
  }

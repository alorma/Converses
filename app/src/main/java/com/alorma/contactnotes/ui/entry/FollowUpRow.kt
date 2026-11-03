package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.flags.FeatureState
import com.alorma.contactnotes.designsystem.components.ConversesSwitch
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ProBadge
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics

/**
 * Fila «Pendent» a sota de «Quan» (P5, 7e). `Unlocked`: interruptor. `Locked`: PRO i `chevron_right`, i tota
 * la fila obre el paywall; si l'entrada ja era pendent, l'interruptor surt encès i només es pot apagar.
 * `Hidden`: no surt.
 */
@Composable
fun FollowUpRow(
  state: FeatureState,
  pending: Boolean,
  onPendingChange: (Boolean) -> Unit,
  onOpenPro: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (state == FeatureState.Hidden) return
  val haptics = rememberConversesHaptics()
  val showSwitch = state == FeatureState.Unlocked || pending
  val subtitle =
    when {
      !showSwitch -> R.string.follow_up_locked_summary
      pending -> R.string.follow_up_on_summary
      else -> R.string.follow_up_off_summary
    }
  val change = { value: Boolean ->
    haptics.toggle(value)
    onPendingChange(value)
  }

  Surface(
    onClick = onOpenPro,
    enabled = !showSwitch,
    shape = MaterialTheme.shapes.extraLarge,
    color = GroupedListDefaults.containerColor,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .then(if (showSwitch) Modifier.toggleable(pending, role = Role.Switch, onValueChange = change) else Modifier)
          .heightIn(min = 72.dp)
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = CircleShape,
        color =
          if (pending) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor =
          if (pending) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(if (pending) Icons.Rounded.Flag else Icons.Outlined.Flag, contentDescription = null)
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(stringResource(R.string.follow_up), fontSize = 17.sp, fontWeight = FontWeight.Bold)
          if (!showSwitch) {
            ProBadge(
              text = stringResource(R.string.pro_badge),
              contentDescription = stringResource(R.string.pro_description),
            )
          }
        }
        Text(stringResource(subtitle), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (showSwitch) {
        ConversesSwitch(checked = pending, onCheckedChange = null)
      } else {
        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
      }
    }
  }
}

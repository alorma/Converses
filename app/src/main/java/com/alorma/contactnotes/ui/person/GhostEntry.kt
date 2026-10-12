package com.alorma.contactnotes.ui.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.designsystem.components.GhostTimelineMarker
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.TimelineRow
import com.alorma.contactnotes.shared.EntryKind

private val OuterCorner = 22.dp
private val InnerCorner = 8.dp

/** Primera entrada fantasma de la cronologia: convida a començar triant qui parla. */
@Composable
internal fun GhostEntry(
  person: PersonEntity,
  firstName: String,
  onNewEntryAs: (EntryKind) -> Unit,
) {
  TimelineRow(
    isFirst = true,
    isLast = false,
    marker = { GhostTimelineMarker(icon = Icons.Rounded.Add) },
    dashedLine = true,
  ) {
    Surface(
      onClick = { onNewEntryAs(EntryKind.SAID) },
      shape = RoundedCornerShape(18.dp),
      color = GroupedListDefaults.containerColor,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = stringResource(R.string.empty_timeline_title),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.empty_timeline_body, firstName),
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val kinds = EntryKind.entries
          kinds.forEachIndexed { index, kind ->
            val label = whoLabel(kind, firstName)
            WhoButton(
              label = label,
              shape = groupShape(index, kinds.size),
              onClick = { onNewEntryAs(kind) },
              modifier = Modifier.weight(1f),
              leading = {
                if (kind == EntryKind.HEARD) {
                  PersonInitialAvatar(person, size = 22.dp)
                } else {
                  kind.whoIcon()?.let { Icon(it, contentDescription = null) }
                }
              },
            )
          }
        }
      }
    }
  }
}

private fun groupShape(
  index: Int,
  count: Int,
): Shape {
  val start = if (index == 0) OuterCorner else InnerCorner
  val end = if (index == count - 1) OuterCorner else InnerCorner
  return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

@Composable
private fun WhoButton(
  label: String,
  shape: Shape,
  onClick: () -> Unit,
  leading: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val description = stringResource(R.string.empty_timeline_new_entry, label)
  Surface(
    onClick = onClick,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier =
      modifier
        .heightIn(min = 44.dp)
        .clearAndSetSemantics {
          contentDescription = description
          role = Role.Button
          onClick {
            onClick()
            true
          }
        },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      leading()
      Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
  }
}

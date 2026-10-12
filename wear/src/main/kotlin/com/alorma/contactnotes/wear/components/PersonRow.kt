package com.alorma.contactnotes.wear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.wear.theme.AvatarColor
import com.alorma.contactnotes.wear.theme.RowSubtitleStyle
import com.alorma.contactnotes.wear.theme.RowTitleStyle
import com.alorma.contactnotes.wear.theme.WearColors

/**
 * Fila de la llista de persones: avatar Galeta, nom i hora de l'última entrada.
 * Una persona protegida només mostra nom i cadenat: cap hora ni entrada.
 */
@Composable
fun PersonRow(
  name: String,
  initials: String,
  color: AvatarColor,
  lastEntry: String?,
  isProtected: Boolean,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .height(52.dp)
        .clip(shape)
        .background(WearColors.SurfaceCard)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PersonAvatar(initials = initials, color = color, size = 36.dp)
    Column(modifier = Modifier.weight(1f)) {
      Text(text = name, style = RowTitleStyle, color = WearColors.OnSurface, maxLines = 1)
      if (!isProtected && lastEntry != null) {
        Text(text = lastEntry, style = RowSubtitleStyle, color = WearColors.OnSurfaceVariant, maxLines = 1)
      }
    }
    if (isProtected) {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = WearColors.OnSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

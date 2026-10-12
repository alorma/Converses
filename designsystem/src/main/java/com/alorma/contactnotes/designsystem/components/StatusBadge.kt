package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.shared.AvatarShape

/** Icona sobre una galeta (la forma dels avatars): serveix per a estats com «Fet» o «Confirma». */
@Composable
fun StatusBadge(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  size: Dp = 112.dp,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(AvatarShape.Cookie9Sided.asShape())
        .background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.42f))
  }
}

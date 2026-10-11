package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Xip d'un canal de la persona: toc per obrir-lo i toc llarg per copiar l'usuari; sense accions és només informatiu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversesChannelChip(
  label: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
) {
  val haptics = rememberConversesHaptics()
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier,
  ) {
    Row(
      modifier =
        Modifier
          .height(32.dp)
          .combinedClickable(
            enabled = onClick != null || onLongClick != null,
            role = Role.Button,
            onClick = { onClick?.invoke() },
            onLongClick =
              onLongClick?.let { longClick ->
                {
                  haptics.longPress()
                  longClick()
                }
              },
          ).padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
  }
}

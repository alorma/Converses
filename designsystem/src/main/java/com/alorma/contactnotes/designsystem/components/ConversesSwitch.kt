package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Únic interruptor de l'app: quan està activat porta una marca dins del cercle. */
@Composable
fun ConversesSwitch(
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    thumbContent = if (checked) {
      {
        Icon(
          Icons.Rounded.Check,
          contentDescription = null,
          modifier = Modifier.size(SwitchDefaults.IconSize),
          // El cercle és `onPrimary`: la marca porta `primary` (fosca en clar, clara en fosc).
          tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
      }
    } else {
      null
    },
  )
}

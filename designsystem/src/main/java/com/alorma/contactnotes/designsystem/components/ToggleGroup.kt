package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class ToggleOption<T>(val value: T, val label: String, val icon: ImageVector)

/** Grup connectat de botons amb selecció única; la icona només es mostra a l'opció seleccionada. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConversesToggleGroup(
  options: List<ToggleOption<T>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  Row(
    modifier = modifier.fillMaxWidth().selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    options.forEachIndexed { index, option ->
      val shapes = when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
      }
      val checked = option.value == selected
      ToggleButton(
        checked = checked,
        onCheckedChange = { onSelect(option.value) },
        modifier = Modifier.weight(1f),
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(
          containerColor = scheme.surfaceContainerHigh,
          contentColor = scheme.onSurface,
          checkedContainerColor = scheme.primary,
          checkedContentColor = scheme.onPrimary,
        ),
      ) {
        if (checked) {
          Icon(option.icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
          Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
        }
        Text(option.label, maxLines = 1)
      }
    }
  }
}

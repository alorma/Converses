package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Fila de cercles de color de 44 dp amb anell i marca a l'element seleccionat. */
@Composable
fun <T> ColorSwatches(
  entries: List<T>,
  selected: T,
  colorOf: (T) -> Color,
  onSelect: (T) -> Unit,
  labelOf: @Composable (T) -> String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().selectableGroup(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    entries.forEach { entry ->
      val isSelected = entry == selected
      val label = labelOf(entry)
      Box(
        modifier =
          Modifier
            .size(44.dp)
            .then(
              if (isSelected) {
                Modifier.border(
                  BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                  CircleShape,
                )
              } else {
                Modifier
              },
            ).padding(5.dp)
            .clip(CircleShape)
            .background(colorOf(entry))
            .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(entry) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
      ) {
        if (isSelected) {
          Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = Color(0xFF1E1D24),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

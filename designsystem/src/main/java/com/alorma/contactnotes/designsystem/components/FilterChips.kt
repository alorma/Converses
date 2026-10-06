package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Xip de filtre: seleccionat en `primary`, la resta en `surfaceContainerHigh` i sense vora. */
@Composable
fun ConversesFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    label = { Text(label) },
    leadingIcon = if (selected) {
      { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
    } else {
      null
    },
    shape = MaterialTheme.shapes.large,
    colors = FilterChipDefaults.filterChipColors(
      containerColor = scheme.surfaceContainerHigh,
      selectedContainerColor = scheme.primary,
      selectedLabelColor = scheme.onPrimary,
      selectedLeadingIconColor = scheme.onPrimary,
    ),
    border = null,
  )
}

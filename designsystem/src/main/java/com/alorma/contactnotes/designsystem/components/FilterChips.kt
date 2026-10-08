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
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Xip de filtre sense vora. Seleccionat en `primary`, o en `primaryContainer` si `tonal`.
 * Sense `icon` mostra un check quan està seleccionat; amb `icon` el mostra sempre.
 */
@Composable
fun ConversesFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  tonal: Boolean = false,
  leadingContent: (@Composable () -> Unit)? = null,
) {
  val scheme = MaterialTheme.colorScheme
  val selectedContainer = if (tonal) scheme.primaryContainer else scheme.primary
  val selectedContent = if (tonal) scheme.onPrimaryContainer else scheme.onPrimary
  val leading = icon ?: if (selected) Icons.Rounded.Check else null
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    label = { Text(label) },
    leadingIcon = leadingContent ?: leading?.let { vector ->
      { Icon(vector, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
    },
    shape = MaterialTheme.shapes.large,
    colors = FilterChipDefaults.filterChipColors(
      containerColor = scheme.surfaceContainerHigh,
      selectedContainerColor = selectedContainer,
      selectedLabelColor = selectedContent,
      selectedLeadingIconColor = selectedContent,
    ),
    border = null,
  )
}

package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Grup connectat de filtres amb selecció única, desplaçable en horitzontal. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConversesConnectedFilterRow(
  options: List<Pair<T, String>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    options.forEachIndexed { index, (value, label) ->
      val shapes = when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
      }
      ToggleButton(
        checked = value == selected,
        onCheckedChange = { onSelect(value) },
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(
          containerColor = scheme.surfaceContainerHigh,
          contentColor = scheme.onSurface,
          checkedContainerColor = scheme.primary,
          checkedContentColor = scheme.onPrimary,
        ),
      ) {
        Text(label, maxLines = 1)
      }
    }
  }
}

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
    leadingIcon = leading?.let { vector ->
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

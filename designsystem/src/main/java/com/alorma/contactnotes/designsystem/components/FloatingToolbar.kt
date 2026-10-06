package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Barra d'eines flotant amb el FAB integrat. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesFloatingToolbar(
  fabIcon: ImageVector,
  fabContentDescription: String,
  onFabClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  HorizontalFloatingToolbar(
    expanded = true,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onFabClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(imageVector = fabIcon, contentDescription = fabContentDescription)
      }
    },
    modifier = modifier,
    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
    content = content,
  )
}

@Composable
fun ToolbarDestination(
  icon: ImageVector,
  contentDescription: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  IconToggleButton(
    checked = selected,
    onCheckedChange = { onClick() },
    modifier = modifier,
    colors = IconButtonDefaults.iconToggleButtonColors(
      checkedContainerColor = MaterialTheme.colorScheme.primary,
      checkedContentColor = MaterialTheme.colorScheme.onPrimary,
    ),
  ) {
    Icon(imageVector = icon, contentDescription = contentDescription)
  }
}

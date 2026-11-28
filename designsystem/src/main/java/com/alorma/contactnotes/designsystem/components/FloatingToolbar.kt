package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.expressive.ShapeFab

/** Barra d'eines flotant a l'esquerra i FAB de nota ràpida a la dreta. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesFloatingToolbar(
  fabIcon: ImageVector,
  fabContentDescription: String,
  onFabClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    HorizontalFloatingToolbar(
      expanded = true,
      content = content,
    )
    FloatingActionButton(
      onClick = onFabClick,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
      Icon(imageVector = fabIcon, contentDescription = fabContentDescription)
    }
  }
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
    colors =
      IconButtonDefaults.iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
      ),
  ) {
    Icon(imageVector = icon, contentDescription = contentDescription)
  }
}

/**
 * Variant expressiva de [ConversesFloatingToolbar]: barra de 68 dp en tinta (`primary`) amb el [ShapeFab] a la dreta.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFloatingToolbar(
  onFabClick: () -> Unit,
  fabIcon: ImageVector,
  fabContentDescription: String,
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    HorizontalFloatingToolbar(
      expanded = true,
      modifier = Modifier.height(68.dp),
      colors =
        FloatingToolbarDefaults.standardFloatingToolbarColors(
          toolbarContainerColor = MaterialTheme.colorScheme.primary,
          toolbarContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      shape = RoundedCornerShape(34.dp),
      content = content,
    )
    ShapeFab(icon = fabIcon, contentDescription = fabContentDescription, onClick = onFabClick)
  }
}

/**
 * Destí de [ExpressiveFloatingToolbar] sobre el `ToggleButton` de Material 3: marcat mostra icona i text sobre
 * `secondaryContainer`; sense marcar, només la icona (amb [label] com a descripció).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveToolbarDestination(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  ToggleButton(
    checked = selected,
    onCheckedChange = { onClick() },
    modifier = modifier,
    colors =
      ToggleButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = scheme.onPrimary,
        checkedContainerColor = scheme.secondaryContainer,
        checkedContentColor = scheme.onSecondaryContainer,
      ),
  ) {
    if (selected) {
      Icon(imageVector = icon, contentDescription = null)
      Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
      Text(label)
    } else {
      Icon(imageVector = icon, contentDescription = label)
    }
  }
}

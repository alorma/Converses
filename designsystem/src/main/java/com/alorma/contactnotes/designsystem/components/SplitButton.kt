package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Split button: acció principal a l'esquerra i fletxa que desplega un menú. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesSplitButton(
  label: String,
  icon: ImageVector,
  onClick: () -> Unit,
  menuExpanded: Boolean,
  onMenuExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  menuContent: @Composable ColumnScope.() -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
  val height = SplitButtonDefaults.MediumContainerHeight
  Box(modifier = modifier) {
    SplitButtonLayout(
      leadingButton = {
        SplitButtonDefaults.LeadingButton(
          onClick = onClick,
          modifier = Modifier.heightIn(min = height),
          shapes = SplitButtonDefaults.leadingButtonShapesFor(height),
          colors = colors,
          contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(height),
        ) {
          Icon(icon, contentDescription = null, modifier = Modifier.size(SplitButtonDefaults.leadingButtonIconSizeFor(height)))
          Spacer(Modifier.width(8.dp))
          Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      },
      trailingButton = {
        SplitButtonDefaults.TrailingButton(
          checked = menuExpanded,
          onCheckedChange = onMenuExpandedChange,
          modifier = Modifier.heightIn(min = height),
          shapes = SplitButtonDefaults.trailingButtonShapesFor(height),
          colors = colors,
          contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(height),
        ) {
          Icon(
            imageVector = if (menuExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(SplitButtonDefaults.trailingButtonIconSizeFor(height)),
          )
        }
      },
    )
    DropdownMenu(
      expanded = menuExpanded,
      onDismissRequest = { onMenuExpandedChange(false) },
      shape = RoundedCornerShape(GroupedListDefaults.OuterCorner),
      containerColor = scheme.surfaceContainerHigh,
      content = menuContent,
    )
  }
}

/** Element del menú del split button: 52 dp d'alçada i icona en `onSurfaceVariant`. */
@Composable
fun ConversesMenuItem(
  text: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenuItem(
    text = { Text(text) },
    leadingIcon = { Icon(icon, contentDescription = null) },
    onClick = onClick,
    modifier = modifier.heightIn(min = 52.dp),
    colors = MenuDefaults.itemColors(leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant),
  )
}

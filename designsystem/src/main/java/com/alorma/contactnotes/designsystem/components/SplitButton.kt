package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
  Box(modifier = modifier) {
    SplitButtonLayout(
      leadingButton = {
        SplitButtonDefaults.LeadingButton(onClick = onClick, colors = colors) {
          Icon(icon, contentDescription = null, modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize))
          Spacer(Modifier.width(8.dp))
          Text(label)
        }
      },
      trailingButton = {
        SplitButtonDefaults.TrailingButton(
          checked = menuExpanded,
          onCheckedChange = onMenuExpandedChange,
          colors = colors,
        ) {
          Icon(
            imageVector = if (menuExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
          )
        }
      },
    )
    DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }, content = menuContent)
  }
}

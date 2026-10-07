package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Una opció del grup. `leading` substitueix la icona (per exemple, un avatar) i `selectedIcon` és la
 * variant farcida que es mostra quan l'opció està seleccionada.
 */
data class ToggleOption<T>(
  val value: T,
  val label: String,
  val icon: ImageVector? = null,
  val selectedIcon: ImageVector? = icon,
  val leading: (@Composable () -> Unit)? = null,
  val enabled: Boolean = true,
  val contentDescription: String? = null,
)

/**
 * Grup connectat de botons amb selecció única. Per defecte la icona només es mostra a l'opció seleccionada;
 * amb `prominent` els botons fan 52 dp, la icona (o el `leading`) sempre és visible i el seleccionat va en negreta.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConversesToggleGroup(
  options: List<ToggleOption<T>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  prominent: Boolean = false,
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
        enabled = option.enabled,
        modifier = Modifier
          .weight(1f)
          .then(if (prominent) Modifier.height(52.dp) else Modifier)
          .semantics {
            role = Role.RadioButton
            option.contentDescription?.let { contentDescription = it }
          },
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(
          containerColor = scheme.surfaceContainerHigh,
          contentColor = scheme.onSurface,
          checkedContainerColor = scheme.primary,
          checkedContentColor = scheme.onPrimary,
        ),
      ) {
        val vector = if (checked) option.selectedIcon else option.icon
        if ((prominent || checked) && (option.leading != null || vector != null)) {
          if (option.leading != null) {
            option.leading.invoke()
          } else if (vector != null) {
            Icon(vector, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
          }
          Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
        }
        Text(
          text = option.label,
          maxLines = 1,
          fontWeight = if (prominent) (if (checked) FontWeight.Bold else FontWeight.Medium) else null,
        )
      }
    }
  }
}

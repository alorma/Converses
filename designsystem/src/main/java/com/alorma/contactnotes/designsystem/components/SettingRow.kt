package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight

/**
 * Fila d'ajustos sobre `surface-card`; la forma ve de la posició dins el grup (24 als extrems, 6 entre files).
 * `modifier` va fora del retall de la forma; el que ha de rebre clics, dins d'ella, va a `contentModifier`.
 */
@Composable
fun SettingRow(
  title: String,
  shape: Shape,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: ImageVector? = null,
  onClick: (() -> Unit)? = null,
  trailing: (@Composable () -> Unit)? = null,
  contentModifier: Modifier = Modifier,
) {
  val content = @Composable {
    ListItem(
      modifier = contentModifier,
      headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
      supportingContent = subtitle?.let { { Text(it) } },
      leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
      trailingContent = trailing,
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
  }
  if (onClick != null) {
    Surface(onClick = onClick, shape = shape, color = GroupedListDefaults.containerColor, modifier = modifier.fillMaxWidth()) {
      content()
    }
  } else {
    Surface(shape = shape, color = GroupedListDefaults.containerColor, modifier = modifier.fillMaxWidth()) { content() }
  }
}

@Composable
fun SettingSwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  shape: Shape,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: ImageVector? = null,
  /** `false` quan el canvi no és immediat (cal desbloquejar abans) i qui crida vibra en aplicar-lo. */
  haptic: Boolean = true,
) {
  val haptics = rememberConversesHaptics()
  SettingRow(
    title = title,
    shape = shape,
    subtitle = subtitle,
    icon = icon,
    modifier = modifier,
    contentModifier = Modifier.toggleable(
      value = checked,
      role = Role.Switch,
      onValueChange = {
        if (haptic) haptics.toggle(it)
        onCheckedChange(it)
      },
    ),
    trailing = { Switch(checked = checked, onCheckedChange = null) },
  )
}

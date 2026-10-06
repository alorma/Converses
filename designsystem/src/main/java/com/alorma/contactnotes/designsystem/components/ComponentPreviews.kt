package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewSurface

// Previews dels components amb dades fixes. Els tests de captures de `screenshotTest` les reutilitzen.

@Preview
@Composable
internal fun AvatarPreview() {
  ConversesPreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Avatar(initials = "MS", shape = AvatarShape.Cookie9Sided, color = AvatarColor.Lavender)
      Avatar(initials = "JP", shape = AvatarShape.Clover4Leaf, color = AvatarColor.Mint)
      Avatar(initials = "LF", shape = AvatarShape.Flower, color = AvatarColor.Peach)
      Avatar(initials = "PV", shape = AvatarShape.Sunny, color = AvatarColor.Sky)
      Avatar(initials = "LR", shape = AvatarShape.Cookie9Sided, color = AvatarColor.Rose, locked = true)
    }
  }
}

@Preview
@Composable
internal fun FilterChipPreview() {
  ConversesPreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ConversesFilterChip(label = "All", selected = true, onClick = {})
      ConversesFilterChip(label = "Chat", selected = false, onClick = {})
      ConversesFilterChip(label = "Said", selected = true, onClick = {}, icon = Icons.Rounded.NorthEast, tonal = true)
      ConversesFilterChip(label = "Heard", selected = false, onClick = {}, icon = Icons.Rounded.SouthWest, tonal = true)
    }
  }
}

@Preview
@Composable
internal fun ToggleGroupPreview() {
  ConversesPreviewSurface {
    ConversesToggleGroup(
      options = listOf(
        ToggleOption("light", "Light", Icons.Rounded.LightMode),
        ToggleOption("dark", "Dark", Icons.Rounded.DarkMode),
        ToggleOption("system", "System", Icons.Rounded.BrightnessAuto),
      ),
      selected = "dark",
      onSelect = {},
      modifier = Modifier.width(360.dp),
    )
  }
}

@Preview
@Composable
internal fun TimelinePreview() {
  ConversesPreviewSurface {
    Column(modifier = Modifier.width(360.dp)) {
      TimelineRow(isFirst = true, isLast = false) {
        Text("14 March 2025", style = MaterialTheme.typography.titleSmall)
      }
      TimelineRow(
        isFirst = false,
        isLast = false,
        marker = { TimelineMarker(icon = Icons.Rounded.NorthEast, style = TimelineMarkerStyle.Filled) },
      ) {
        Text("Told her I can't make it to dinner on Saturday.", style = MaterialTheme.typography.bodyLarge)
      }
      TimelineRow(
        isFirst = false,
        isLast = false,
        marker = { TimelineMarker(icon = Icons.Rounded.SouthWest, style = TimelineMarkerStyle.Tonal) },
      ) {
        Text("She told me she moved flats.", style = MaterialTheme.typography.bodyLarge)
      }
      TimelineRow(
        isFirst = false,
        isLast = true,
        marker = { TimelineMarker(icon = Icons.Rounded.Event, style = TimelineMarkerStyle.Outlined) },
      ) {
        Text("We went to the photography exhibition.", style = MaterialTheme.typography.bodyLarge)
      }
    }
  }
}

@Preview
@Composable
internal fun SplitButtonPreview() {
  ConversesPreviewSurface {
    ConversesSplitButton(
      label = "Note",
      icon = Icons.Rounded.Edit,
      onClick = {},
      menuExpanded = false,
      onMenuExpandedChange = {},
      menuContent = {},
    )
  }
}

@Preview
@Composable
internal fun ColorSwatchesPreview() {
  ConversesPreviewSurface {
    ColorSwatches(
      entries = AvatarColor.entries,
      selected = AvatarColor.Mint,
      colorOf = { it.color },
      onSelect = {},
      modifier = Modifier.width(360.dp),
    )
  }
}

@Preview
@Composable
internal fun FloatingToolbarPreview() {
  ConversesPreviewSurface {
    ConversesFloatingToolbar(
      fabIcon = Icons.Rounded.EditNote,
      fabContentDescription = "Quick note",
      onFabClick = {},
      modifier = Modifier.width(360.dp),
    ) {
      ToolbarDestination(icon = Icons.Rounded.Groups, contentDescription = "People", selected = true, onClick = {})
      ToolbarDestination(icon = Icons.Rounded.Settings, contentDescription = "Settings", selected = false, onClick = {})
    }
  }
}

@Preview
@Composable
internal fun GroupedListPreview() {
  ConversesPreviewSurface {
    GroupedList(items = listOf("Birthday", "Work", "Allergies"), modifier = Modifier.width(360.dp)) { label ->
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      )
    }
  }
}

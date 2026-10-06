package com.alorma.contactnotes.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import com.alorma.contactnotes.designsystem.components.GroupedList
import com.alorma.contactnotes.designsystem.theme.ColorStyle
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.designsystem.theme.ThemeMode

@Composable
fun ThemeGalleryScreen(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  colorStyle: ColorStyle,
  onColorStyleChange: (ColorStyle) -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier
        .safeDrawingPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(stringResource(R.string.gallery_title), style = ScreenTitleStyle)

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
          FilterChip(
            selected = mode == themeMode,
            onClick = { onThemeModeChange(mode) },
            label = {
              Text(
                stringResource(
                  when (mode) {
                    ThemeMode.System -> R.string.gallery_theme_system
                    ThemeMode.Light -> R.string.gallery_theme_light
                    ThemeMode.Dark -> R.string.gallery_theme_dark
                  },
                ),
              )
            },
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorStyle.entries.forEach { style ->
          FilterChip(
            selected = style == colorStyle,
            onClick = { onColorStyleChange(style) },
            label = {
              Text(
                stringResource(
                  when (style) {
                    ColorStyle.Tinted -> R.string.gallery_style_tinted
                    ColorStyle.Pure -> R.string.gallery_style_pure
                  },
                ),
              )
            },
          )
        }
      }

      Text(stringResource(R.string.gallery_section_avatars), style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val colors = AvatarColor.entries
        AvatarShape.entries.forEachIndexed { index, shape ->
          Avatar(initials = "AB", shape = shape, color = colors[index % colors.size], size = 64.dp)
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AvatarColor.entries.forEach { color ->
          Avatar(initials = "C", shape = AvatarShape.Cookie9Sided, color = color, size = 40.dp)
        }
      }
      Text(stringResource(R.string.gallery_person_name), style = PersonNameStyle)

      Text(stringResource(R.string.gallery_section_list), style = MaterialTheme.typography.titleMedium)
      GroupedList(
        items = listOf(
          R.string.gallery_row_one,
          R.string.gallery_row_two,
          R.string.gallery_row_three,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) { label ->
        Text(
          text = stringResource(label),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        )
      }

      Text(stringResource(R.string.gallery_section_error), style = MaterialTheme.typography.titleMedium)
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(start = 16.dp, end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.gallery_error_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = {}) { Text(stringResource(R.string.gallery_error_retry)) }
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

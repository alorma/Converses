package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.components.PersonAvatar
import com.alorma.contactnotes.wear.components.WhoGroup
import com.alorma.contactnotes.wear.components.WhoOption
import com.alorma.contactnotes.wear.theme.ButtonLabelStyle
import com.alorma.contactnotes.wear.theme.WearColors
import com.alorma.contactnotes.wear.theme.WhoTitleStyle

/** Pantalla 4: Jo / {persona} / Apunt; per defecte, la persona (índex 1). */
@Composable
fun WhoContent(
  personName: String,
  personInitials: String,
  personColor: AvatarColor,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = rememberTransformingLazyColumnState()
  val options =
    listOf(
      WhoOption(
        label = stringResource(R.string.entry_who_me),
        leading = { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(24.dp)) },
      ),
      WhoOption(
        label = personName,
        leading = { PersonAvatar(initials = personInitials, color = personColor, size = 28.dp) },
        leadingIsAvatar = true,
      ),
      WhoOption(
        label = stringResource(R.string.entry_who_note),
        leading = { Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(24.dp)) },
      ),
    )
  ScreenScaffold(
    scrollState = state,
    modifier = modifier,
  ) {
    TransformingLazyColumn(
      state = state,
      contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 30.dp, bottom = PinnedEdgeButtonSpace),
    ) {
      item {
        Text(
          text = stringResource(R.string.who_title),
          style = WhoTitleStyle,
          color = WearColors.OnSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
      }
      item { WhoGroup(options = options, selectedIndex = selectedIndex, onSelect = onSelect) }
    }
    PinnedEdgeButton(onClick = onSave) { Text(text = stringResource(R.string.save), style = ButtonLabelStyle) }
  }
}

@WearScreenPreviews
@Composable
fun WhoPreview() =
  WearPreview {
    var selected by remember { mutableIntStateOf(1) }
    WhoContent(
      personName = "Kael",
      personInitials = "K",
      personColor = AvatarColor.Sky,
      selectedIndex = selected,
      onSelect = { selected = it },
      onSave = {},
    )
  }

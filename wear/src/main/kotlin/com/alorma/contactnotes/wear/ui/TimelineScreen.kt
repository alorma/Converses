package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.components.EntryAuthor
import com.alorma.contactnotes.wear.components.PersonAvatar
import com.alorma.contactnotes.wear.components.TimelineEntry
import com.alorma.contactnotes.wear.theme.ButtonLabelStyle
import com.alorma.contactnotes.wear.theme.PersonNameStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Entrada ja formatada per mostrar: qui, «qui · hora · canal» i text. */
data class EntryItem(
  val id: Long,
  val author: EntryAuthor,
  val meta: String,
  val text: String,
)

/** Pantalla 2: cronologia d'una persona (només lectura) amb l'`EdgeButton` «Nota». */
@Composable
fun TimelineContent(
  name: String,
  initials: String,
  color: AvatarColor,
  entries: List<EntryItem>,
  onNote: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (entries.isEmpty()) {
    EmptyTimelineContent(name = name, initials = initials, color = color, onNote = onNote, modifier = modifier)
    return
  }
  val state = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()
  ScreenScaffold(
    scrollState = state,
    modifier = modifier,
  ) {
    TransformingLazyColumn(
      state = state,
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = PinnedEdgeButtonSpace),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PersonAvatar(initials = initials, color = color, size = 30.dp)
          Text(text = name, style = PersonNameStyle, color = WearColors.OnSurface, maxLines = 1)
        }
      }
      items(entries.size, key = { entries[it].id }) { index ->
        val entry = entries[index]
        TimelineEntry(
          author = entry.author,
          meta = entry.meta,
          text = entry.text,
          modifier = Modifier.scrollTransformed(this, spec),
        )
      }
    }
    PinnedEdgeButton(onClick = onNote) {
      Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
      Text(text = stringResource(R.string.note_button), style = ButtonLabelStyle)
    }
  }
}

@Composable
private fun sampleEntries(): List<EntryItem> {
  val me = stringResource(R.string.entry_who_me)
  val chat = stringResource(R.string.channel_chat)
  return listOf(
    EntryItem(1, EntryAuthor.Me, "$me · 21:40 · $chat", "Perfecte, a les cinc al parc."),
    EntryItem(2, EntryAuthor.Person("K", AvatarColor.Sky), "Kael · 21:36 · $chat", "Dissabte em va bé."),
    EntryItem(3, EntryAuthor.Me, "$me · 21:30 · $chat", "Et ve de gust anar al parc?"),
  )
}

@WearScreenPreviews
@Composable
fun TimelinePreview() =
  WearPreview {
    TimelineContent(name = "Kael", initials = "K", color = AvatarColor.Sky, entries = sampleEntries(), onNote = {})
  }

/** Pantalla 2 connectada al ViewModel; si la persona ja no és disponible, torna enrere. */
@Composable
fun TimelineScreen(
  viewModel: TimelineViewModel,
  onBack: () -> Unit,
  onNote: () -> Unit,
) {
  when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
    TimelineUiState.Loading -> Unit
    TimelineUiState.Unavailable -> LaunchedEffect(Unit) { onBack() }
    is TimelineUiState.Loaded ->
      TimelineContent(
        name = state.name,
        initials = state.initials,
        color = state.color,
        entries = state.entries,
        onNote = onNote,
      )
  }
}

@WearScreenPreviews
@Composable
fun TimelineEmptyPreview() =
  WearPreview {
    TimelineContent(name = "Kael", initials = "K", color = AvatarColor.Sky, entries = emptyList(), onNote = {})
  }

/** P3: sense entrades només hi ha el botó «Nota», gran i al centre. */
@Composable
private fun EmptyTimelineContent(
  name: String,
  initials: String,
  color: AvatarColor,
  onNote: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ScreenScaffold(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(initials = initials, color = color, size = 30.dp)
        Text(text = name, style = PersonNameStyle, color = WearColors.OnSurface, maxLines = 1)
      }
      Button(onClick = onNote) {
        Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
        Text(text = stringResource(R.string.note_button), style = ButtonLabelStyle)
      }
    }
  }
}

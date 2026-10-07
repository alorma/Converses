package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.ui.preview.PreviewData

private val Channels = PreviewData.martaChannels.map { it.type }

@Composable
private fun QuickNotePreview(state: QuickNoteUiState, form: EntryFormState) {
  ConversesPreviewTheme {
    QuickNoteContent(
      state = state,
      form = form,
      onClose = {},
      onSelectPerson = {},
      onNewPerson = {},
      onSave = { _, _ -> },
    )
  }
}

@Preview
@Composable
internal fun QuickNoteNoPersonPreview() {
  QuickNotePreview(
    state = QuickNoteUiState(people = listOf(PreviewData.marta)),
    form = EntryFormState(whenChoice = WhenChoice.Stored(PreviewData.formTime)),
  )
}

@Preview
@Composable
internal fun QuickNoteFilledPreview() {
  QuickNotePreview(
    state = QuickNoteUiState(
      people = listOf(PreviewData.marta),
      selected = PreviewData.marta,
      channels = Channels,
      lastChannel = Channels.first(),
    ),
    form = EntryFormState(
      kind = EntryKind.HEARD,
      text = "She can't come on Saturday.",
      whenChoice = WhenChoice.Custom(PreviewData.formTime),
    ),
  )
}

@Preview
@Composable
internal fun NewEntrySheetPreview() {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.avatarColor) {
      Surface(color = MaterialTheme.colorScheme.surface) {
        NewEntrySheetContent(
          person = PreviewData.marta,
          form = EntryFormState(
            kind = EntryKind.HAPPENED,
            text = "We went to the photography exhibition.",
            whenChoice = WhenChoice.Stored(PreviewData.formTime),
          ),
          available = availableChannels(Channels),
          channel = Channels.first(),
          isNew = true,
          onDismiss = {},
          onSave = {},
        )
      }
    }
  }
}

@Composable
private fun WhenSheetPreview(choice: WhenChoice) {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        WhenSheetContent(choice = choice, now = PreviewData.formTime, onChoice = {}, onPick = {})
      }
    }
  }
}

@Preview
@Composable
internal fun WhenSheetNowPreview() = WhenSheetPreview(WhenChoice.Now)

@Preview
@Composable
internal fun WhenSheetCustomPreview() = WhenSheetPreview(WhenChoice.Custom(PreviewData.formTime - 3 * 24 * 60 * 60 * 1000L))

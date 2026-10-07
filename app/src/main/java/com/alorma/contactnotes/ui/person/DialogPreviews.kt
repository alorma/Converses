package com.alorma.contactnotes.ui.person

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.ui.preview.PreviewData

@Preview
@Composable
internal fun ConfirmDeleteDialogPreview() {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.avatarColor) {
      ConfirmDeleteDialog(
        title = "Delete the whole conversation?",
        text = "The 3 entries from this day on WhatsApp will be deleted.",
        onDismiss = {},
        onConfirm = {},
      )
    }
  }
}

@Preview
@Composable
internal fun MoveEntryDialogPreview() {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.avatarColor) {
      MoveEntryDialog(people = PreviewData.people.map { it.person }.drop(1), onDismiss = {}, onSelect = {})
    }
  }
}

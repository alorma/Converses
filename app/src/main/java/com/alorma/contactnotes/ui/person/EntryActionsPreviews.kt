package com.alorma.contactnotes.ui.person

import com.alorma.contactnotes.data.db.color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.ui.preview.PreviewData

@Composable
private fun EntryActionsPreview(dayCount: Int) {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.color) {
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box {
          EntryActionsContent(
            entry = PreviewData.martaEntries.first(),
            firstName = firstName(PreviewData.marta.name),
            dayCount = dayCount,
            onEdit = {},
            onCopy = {},
            onMove = {},
            onDelete = {},
            onDeleteDay = {},
          )
        }
      }
    }
  }
}

/** Una sola entrada aquell dia: no hi ha «Esborrar tota la conversa». */
@Preview
@Composable
internal fun EntryActionsSinglePreview() = EntryActionsPreview(dayCount = 1)

@Preview
@Composable
internal fun EntryActionsDayPreview() = EntryActionsPreview(dayCount = 3)

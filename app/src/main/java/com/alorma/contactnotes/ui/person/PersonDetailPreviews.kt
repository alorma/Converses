package com.alorma.contactnotes.ui.person

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.preview.PreviewData

@Composable
private fun PersonDetailScreenPreview(state: PersonDetailUiState) {
  ConversesPreviewTheme {
    PersonDetailScreen(
      state = state,
      menuExpanded = false,
      onMenuExpandedChange = {},
      onBack = {},
      onKindToggled = {},
      onNewEntry = {},
      onEntryMenu = {},
      onEditPerson = {},
      onExport = {},
      onToggleArchived = {},
      onChannelOpen = {},
      onChannelCopy = {},
      snackbarHostState = remember { SnackbarHostState() },
    )
  }
}

@Preview
@Composable
internal fun PersonDetailScreenLoadedPreview() {
  PersonDetailScreenPreview(
    PersonDetailUiState(
      person = PreviewData.marta,
      channels = PreviewData.martaChannels,
      facts = PreviewData.martaFacts,
      entries = PreviewData.martaEntries,
      saidCount = 1,
      heardCount = 1,
      happenedCount = PreviewData.martaEntries.count { it.kind == EntryKind.HAPPENED },
      isLoading = false,
    ),
  )
}

@Preview
@Composable
internal fun PersonDetailScreenEmptyPreview() {
  PersonDetailScreenPreview(PersonDetailUiState(person = PreviewData.marta, isLoading = false))
}

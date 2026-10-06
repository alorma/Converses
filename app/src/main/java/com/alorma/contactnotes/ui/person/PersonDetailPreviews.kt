package com.alorma.contactnotes.ui.person

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.preview.PreviewData

@Composable
private fun PersonDetailScreenPreview(state: PersonDetailUiState) {
  ConversesPreviewTheme {
    PersonTheme(state.person?.avatarColor) {
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

internal class AvatarColorProvider : PreviewParameterProvider<AvatarColor> {
  override val values: Sequence<AvatarColor> = AvatarColor.entries.asSequence()
}

/** El detall amb el tema de cada un dels 7 colors de persona. */
@Preview
@Composable
internal fun PersonDetailScreenThemedPreview(@PreviewParameter(AvatarColorProvider::class) color: AvatarColor) {
  PersonDetailScreenPreview(
    PersonDetailUiState(
      person = PreviewData.marta.copy(avatarColor = color),
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

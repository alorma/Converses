package com.alorma.contactnotes.ui.people

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.preview.PreviewData

@Composable
private fun PeopleListScreenPreview(state: PeopleListUiState) {
  ConversesPreviewTheme {
    PeopleListScreen(
      state = state,
      onFilterSelected = {},
      onPersonClick = {},
      onQuickNote = {},
      onOpenSettings = {},
      onNewPerson = {},
    )
  }
}

@Preview
@Composable
internal fun PeopleListScreenLoadedPreview() {
  PeopleListScreenPreview(
    PeopleListUiState(people = PreviewData.people, total = PreviewData.people.size, isLoading = false),
  )
}

@Preview
@Composable
internal fun PeopleListScreenEmptyPreview() {
  PeopleListScreenPreview(PeopleListUiState(isLoading = false))
}

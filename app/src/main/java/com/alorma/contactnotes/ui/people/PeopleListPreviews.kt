package com.alorma.contactnotes.ui.people

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.preview.PreviewData

@Composable
private fun PeopleListScreenPreview(
  state: PeopleListUiState,
  archived: Boolean = false,
) {
  ConversesPreviewTheme {
    PeopleListScreen(
      state = state,
      archived = archived,
      onFilterToggled = {},
      onPersonClick = {},
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

/** Panell de llistat d'una pantalla gran: xips en dues línies, avatars de 48 dp i la tercera persona oberta. */
@Preview(widthDp = 380, heightDp = 800)
@Composable
internal fun PeopleListScreenPanePreview() {
  ConversesPreviewTheme {
    PeopleListScreen(
      state = PeopleListUiState(people = PreviewData.people, total = PreviewData.people.size, isLoading = false),
      archived = false,
      onFilterToggled = {},
      onPersonClick = {},
      onNewPerson = {},
      inPane = true,
      selectedPersonId = PreviewData.people[2].person.id,
    )
  }
}

@Preview
@Composable
internal fun PeopleListScreenEmptyPreview() {
  PeopleListScreenPreview(PeopleListUiState(isLoading = false))
}

@Preview
@Composable
internal fun PeopleListScreenArchivedPreview() {
  PeopleListScreenPreview(
    PeopleListUiState(people = PreviewData.people.take(2), total = 2, isLoading = false),
    archived = true,
  )
}

/** 9g: comptador de tasques pendents; la protegida (Luna) no el mostra encara que en tingui. */
@Preview
@Composable
internal fun PeopleListScreenTasksPreview() {
  val counts = listOf(2, 3, 1)
  val people = PreviewData.people.mapIndexed { index, item -> item.copy(openTasks = counts.getOrElse(index) { 0 }) }
  PeopleListScreenPreview(
    PeopleListUiState(people = people, total = people.size, isLoading = false),
  )
}

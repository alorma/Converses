package com.alorma.contactnotes.ui.people

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.preview.PreviewData
import com.alorma.contactnotes.ui.sync.SyncIndicatorState

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
    PeopleListUiState(people = PreviewData.listPeople, total = PreviewData.listPeople.size, isLoading = false),
  )
}

/** Panell de llistat d'una pantalla gran: xips en dues línies, avatars de 48 dp i la tercera persona oberta. */
@Preview(widthDp = 380, heightDp = 800)
@Composable
internal fun PeopleListScreenPanePreview() {
  ConversesPreviewTheme {
    PeopleListScreen(
      state =
        PeopleListUiState(
          people = PreviewData.listPeople,
          total = PreviewData.listPeople.size,
          isLoading = false,
        ),
      archived = false,
      onFilterToggled = {},
      onPersonClick = {},
      onNewPerson = {},
      inPane = true,
      selectedPersonId = PreviewData.listPeople[2].person.id,
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
    PeopleListUiState(people = PreviewData.listPeople.take(2), total = 2, isLoading = false),
    archived = true,
  )
}

/** 9g: comptador de tasques pendents; la protegida (Luna) no el mostra encara que en tingui. */
@Preview
@Composable
internal fun PeopleListScreenTasksPreview() {
  val counts = listOf(2, 3, 1, 0, 0, 4)
  val people = PreviewData.listPeople.mapIndexed { index, item -> item.copy(openTasks = counts.getOrElse(index) { 0 }) }
  PeopleListScreenPreview(
    PeopleListUiState(people = people, total = people.size, isLoading = false),
  )
}

/** 13c: dispositiu tret, l'avís `err-c` fix sota la capçalera. */
@Preview
@Composable
internal fun PeopleListScreenSyncRemovedPreview() {
  ConversesPreviewTheme {
    PeopleListScreen(
      state =
        PeopleListUiState(
          people = PreviewData.listPeople,
          total = PreviewData.listPeople.size,
          isLoading = false,
        ),
      archived = false,
      onFilterToggled = {},
      onPersonClick = {},
      onNewPerson = {},
      syncIndicator = SyncIndicatorState.Removed("B"),
      syncRemoved = true,
    )
  }
}

/** Amb SyncFlag encès (13a): l'indicador a la dreta de la capçalera, aquí amb sessió i falten les paraules. */
@Preview
@Composable
internal fun PeopleListScreenSyncPreview() {
  ConversesPreviewTheme {
    PeopleListScreen(
      state =
        PeopleListUiState(
          people = PreviewData.listPeople,
          total = PreviewData.listPeople.size,
          isLoading = false,
        ),
      archived = false,
      onFilterToggled = {},
      onPersonClick = {},
      onNewPerson = {},
      syncIndicator = SyncIndicatorState.NeedsKey("B"),
    )
  }
}

/** La targeta destacada amb la lletra al doble i una cita llarga: creix, la cita es talla i la galeta es queda a baix. */
@Preview
@Composable
internal fun PeopleListScreenFeaturedHugeFontPreview() {
  val newest = checkNotNull(PreviewData.listPeople.featured())
  val long =
    "We talked for hours about the trip, the budget, who would drive, where we would sleep and what " +
      "to do if it rained all week, and in the end nothing was decided."
  val people =
    PreviewData.listPeople.map {
      if (it.person.id == newest.person.id) it.copy(lastEntry = it.lastEntry?.copy(text = long)) else it
    }
  PeopleListScreenPreview(PeopleListUiState(people = people, total = people.size, isLoading = false))
}

/** La persona de l'entrada més recent és protegida: la targeta no en mostra res i surt la següent. */
@Preview
@Composable
internal fun PeopleListScreenProtectedNewestPreview() {
  val newest = checkNotNull(PreviewData.listPeople.featured())
  val people =
    PreviewData.listPeople.map {
      if (it.person.id == newest.person.id) it.copy(person = it.person.copy(isProtected = true)) else it
    }
  PeopleListScreenPreview(PeopleListUiState(people = people, total = people.size, isLoading = false))
}

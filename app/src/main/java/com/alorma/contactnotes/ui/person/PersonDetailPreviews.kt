package com.alorma.contactnotes.ui.person

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.ui.preview.PreviewData
import com.alorma.contactnotes.ui.task.TasksSection

@Composable
private fun PersonDetailScreenPreview(
  state: PersonDetailUiState,
  isPro: Boolean = false,
  inPane: Boolean = false,
) {
  ConversesPreviewTheme {
    PersonTheme(state.person?.color) {
      PersonDetailScreen(
        state = state,
        menuExpanded = false,
        onMenuExpandedChange = {},
        onBack = {},
        onKindToggled = {},
        onNewEntry = {},
        onNewEntryAs = {},
        onEditFocus = {},
        onEntryMenu = {},
        onEditPerson = {},
        onExport = {},
        onToggleArchived = {},
        snackbarHostState = remember { SnackbarHostState() },
        isPro = isPro,
        today = PreviewData.taskToday,
        inPane = inPane,
      )
    }
  }
}

/** Panell de detall d'una tauleta horitzontal (12a): capçalera amb les accions i dues columnes. */
@Preview(widthDp = 764, heightDp = 768)
@Composable
internal fun PersonDetailScreenTwoColumnsPreview() {
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
    isPro = true,
    inPane = true,
  )
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

/** Persona amb una sola dada: la fila no es pot plegar. */
@Preview
@Composable
internal fun PersonDetailScreenSingleFactPreview() {
  PersonDetailScreenPreview(
    PersonDetailUiState(
      person = PreviewData.marta,
      channels = PreviewData.martaChannels,
      facts = PreviewData.martaFacts.take(1),
      entries = PreviewData.martaEntries,
      saidCount = 1,
      heardCount = 1,
      happenedCount = PreviewData.martaEntries.count { it.kind == EntryKind.HAPPENED },
      isLoading = false,
    ),
  )
}

private val martaWithFactsState =
  PersonDetailUiState(
    person = PreviewData.marta,
    channels = PreviewData.martaChannels,
    facts = PreviewData.martaFacts,
    entries = PreviewData.martaEntries,
    saidCount = 1,
    heardCount = 1,
    happenedCount = PreviewData.martaEntries.count { it.kind == EntryKind.HAPPENED },
    isLoading = false,
  )

internal class AvatarColorProvider : PreviewParameterProvider<AvatarColor> {
  override val values: Sequence<AvatarColor> = AvatarColor.entries.asSequence()
}

/** El detall amb el tema de cada un dels 7 colors de persona. */
@Preview
@Composable
internal fun PersonDetailScreenThemedPreview(
  @PreviewParameter(AvatarColorProvider::class) color: AvatarColor,
) {
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

/** Hi ha entrades, però el filtre actiu no en té cap. */
@Preview
@Composable
internal fun PersonDetailScreenFilteredEmptyPreview() {
  PersonDetailScreenPreview(
    PersonDetailUiState(
      person = PreviewData.marta,
      channels = PreviewData.martaChannels,
      facts = PreviewData.martaFacts,
      entries = emptyList(),
      selectedKinds = setOf(EntryKind.SAID),
      saidCount = 1,
      heardCount = 1,
      happenedCount = 1,
      isLoading = false,
    ),
  )
}

/** Cronologia amb un canal propi: l'etiqueta diu WHATSAPP en lloc del grup. */
@Preview
@Composable
internal fun PersonDetailScreenCustomChannelPreview() {
  val entries =
    PreviewData.martaEntries.mapIndexed { index, entry ->
      if (index ==
        0
      ) {
        entry.copy(customChannelId = 1)
      } else {
        entry
      }
    }
  PersonDetailScreenPreview(
    PersonDetailUiState(
      person = PreviewData.marta,
      channels = PreviewData.martaChannels,
      entries = entries,
      customChannelNames = mapOf(1L to "WhatsApp"),
      saidCount = 1,
      heardCount = 1,
      happenedCount = entries.count { it.kind == EntryKind.HAPPENED },
      isLoading = false,
    ),
  )
}

private val martaTasksState =
  PersonDetailUiState(
    person = PreviewData.marta,
    channels = PreviewData.martaChannels,
    facts = PreviewData.martaFacts,
    entries = PreviewData.martaEntries,
    openTasks = PreviewData.martaOpenTasks,
    doneTasks = PreviewData.martaDoneTasks,
    saidCount = 1,
    heardCount = 1,
    happenedCount = PreviewData.martaEntries.count { it.kind == EntryKind.HAPPENED },
    isLoading = false,
  )

/** 9d: secció «Pendents» amb Pro, amb «Fets · 2» plegat. */
@Preview
@Composable
internal fun PersonDetailTasksPreview() {
  PersonDetailScreenPreview(martaTasksState, isPro = true)
}

/** 9h: amb Pro i sense cap tasca, la targeta buida. */
@Preview
@Composable
internal fun PersonDetailTasksEmptyPreview() {
  PersonDetailScreenPreview(
    martaTasksState.copy(openTasks = emptyList(), doneTasks = emptyList()),
    isPro = true,
  )
}

/** Ha deixat de ser Pro: les tasques es veuen, sense «+ Nova tasca». */
@Preview
@Composable
internal fun PersonDetailTasksLockedPreview() {
  PersonDetailScreenPreview(martaTasksState, isPro = false)
}

/** 9f: «Fets» desplegat (text ratllat, «FET AHIR», «FET EL …»). */
@Preview
@Composable
internal fun TasksSectionDonePreview() {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.color) {
      Surface(color = MaterialTheme.colorScheme.surface) {
        TasksSection(
          open = PreviewData.martaOpenTasks,
          done = PreviewData.martaDoneTasks,
          canCreate = true,
          today = PreviewData.taskToday,
          onNewTask = {},
          onOpen = {},
          onDone = {},
          onReopen = {},
          initialDoneExpanded = true,
          modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
        )
      }
    }
  }
}

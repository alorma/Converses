package com.alorma.contactnotes.ui.task

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.preview.PreviewData
import java.time.LocalDate

/** Dilluns: el dia rellevant és divendres, com al disseny. */
private val PreviewToday = LocalDate.of(2026, 11, 2)

@Composable
private fun TaskFormPreview(
  form: TaskFormState,
  task: TaskEntity? = null,
) {
  ConversesPreviewTheme {
    TaskFormContent(
      state =
        TaskFormUiState(
          people = listOf(PreviewData.marta),
          selected = PreviewData.marta,
          task = task,
          loaded = true,
        ),
      form = form,
      isNew = task == null,
      today = PreviewToday,
      onClose = {},
      onSelectPerson = {},
      onNewPerson = {},
      onSave = {},
      onDelete = {},
    )
  }
}

/** 9b: buit, «Sense data» per defecte i el botó desactivat. */
@Preview
@Composable
internal fun TaskFormEmptyPreview() {
  TaskFormPreview(form = TaskFormState())
}

/** 9c: amb text i el proper dia rellevant (sense «Avisa'm», que surt més endavant). */
@Preview
@Composable
internal fun TaskFormFilledPreview() {
  TaskFormPreview(form = TaskFormState("Ask her how the interview went.", PreviewToday.plusDays(4)))
}

/** Edició amb una data que no és cap dels xips: surt a «Tria dia…», i hi ha l'opció d'esborrar. */
@Preview
@Composable
internal fun TaskFormEditPreview() {
  val task =
    TaskEntity(
      id = 1,
      personId = PreviewData.marta.id,
      text = "Return her book.",
      dueDate = PreviewToday.plusDays(20).toEpochDay(),
      createdAt = 0,
    )
  TaskFormPreview(form = TaskFormState(task.text, LocalDate.ofEpochDay(task.dueDate!!)), task = task)
}

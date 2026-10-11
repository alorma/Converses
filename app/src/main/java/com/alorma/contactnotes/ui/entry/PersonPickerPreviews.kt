package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.AppColors
import com.alorma.contactnotes.ui.preview.PreviewData

@Composable
private fun PersonPickerPreview(
  people: List<PersonEntity>,
  selectedId: Long?,
) {
  ConversesPreviewTheme {
    AppColors {
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(contentAlignment = Alignment.TopStart) {
          PersonPickerContent(people = people, selectedId = selectedId, onSelect = {}, onNewPerson = {})
        }
      }
    }
  }
}

@Preview
@Composable
internal fun PersonPickerFewPreview() {
  PersonPickerPreview(people = PreviewData.people.map { it.person }, selectedId = PreviewData.marta.id)
}

/** Amb més de 8 persones apareix el camp per filtrar pel nom. */
@Preview
@Composable
internal fun PersonPickerManyPreview() {
  val many =
    PreviewData.people.map { it.person } +
      (5L..10L).map { PreviewData.marta.copy(id = it, name = "Person $it") }
  PersonPickerPreview(people = many, selectedId = null)
}

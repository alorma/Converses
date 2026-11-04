package com.alorma.contactnotes.ui.consent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Composable
private fun ConsentSheetPreview(choice: Boolean?) {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      ConsentSheetContent(choice = choice, onChoiceChange = {}, onConfirm = {})
    }
  }
}

/** Full de consentiment sense cap opció triada: «D'acord» desactivat (10a). */
@Preview
@Composable
internal fun ConsentSheetUnchosenPreview() = ConsentSheetPreview(choice = null)

/** Amb «Sí» triat (10b). */
@Preview
@Composable
internal fun ConsentSheetYesPreview() = ConsentSheetPreview(choice = true)

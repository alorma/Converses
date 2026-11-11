package com.alorma.contactnotes.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

private val PreviewAccount = SyncAccount(uid = "uid", name = "Anna", email = "anna@example.com")

private const val PreviewWords = "harbor harvest hazard abandon ability able about above absent absorb abstract"

@Composable
private fun EnterWordsPreview(state: EnterWordsUiState) {
  ConversesPreviewTheme {
    EnterWordsContent(
      state = state,
      onClose = {},
      onPaste = {},
      onTextChange = {},
      onFocusChange = {},
      onSuggestion = {},
      onSubmit = {},
    )
  }
}

/** 13i: el compte ja té dades i falta la clau. */
@Preview
@Composable
internal fun SyncNeedsKeyPreview() {
  ConversesPreviewTheme {
    SyncScaffold(onBack = {}) { padding ->
      SyncNeedsKeyContent(account = PreviewAccount, padding = padding, onEnterWords = {}, onChangeAccount = {})
    }
  }
}

/** 13j: el camp buit. */
@Preview
@Composable
internal fun EnterWordsEmptyPreview() {
  EnterWordsPreview(EnterWordsUiState())
}

/** 13j: s'escriu l'última paraula, amb suggeriments, i una paraula completa que no és a la llista. */
@Preview
@Composable
internal fun EnterWordsTypingPreview() {
  val text = PreviewWords.replace("abandon", "abandn") + " abs"
  EnterWordsPreview(EnterWordsUiState(text = text, focused = true))
}

/** 13j2: dotze paraules que no obren el compte. */
@Preview
@Composable
internal fun EnterWordsWrongPreview() {
  EnterWordsPreview(EnterWordsUiState(text = "$PreviewWords absurd", wrongWords = true))
}

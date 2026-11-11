package com.alorma.contactnotes.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

private val PreviewAccount = SyncAccount(uid = "uid", name = "Anna", email = "anna@example.com")

private val PreviewWords =
  listOf(
    "harbor",
    "harvest",
    "hazard",
    "abandon",
    "ability",
    "able",
    "about",
    "above",
    "absent",
    "absorb",
    "abstract",
    "",
  )

@Composable
private fun EnterWordsPreview(state: EnterWordsUiState) {
  ConversesPreviewTheme {
    EnterWordsContent(
      state = state,
      onClose = {},
      onPaste = { null },
      onFocus = {},
      onBlur = {},
      onWordChange = { _, _ -> null },
      onSuggestion = { _, _ -> null },
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

/** 13j: els dotze camps buits. */
@Preview
@Composable
internal fun EnterWordsEmptyPreview() {
  EnterWordsPreview(EnterWordsUiState())
}

/** 13j: s'escriu el dotzè camp, amb suggeriments, i una paraula que no és a la llista. */
@Preview
@Composable
internal fun EnterWordsTypingPreview() {
  val words =
    PreviewWords.toMutableList().also {
      it[3] = "abandn"
      it[11] = "abs"
    }
  EnterWordsPreview(EnterWordsUiState(words = words, focused = 11))
}

/** 13j2: dotze paraules que no obren el compte. */
@Preview
@Composable
internal fun EnterWordsWrongPreview() {
  val words = PreviewWords.toMutableList().also { it[11] = "absurd" }
  EnterWordsPreview(EnterWordsUiState(words = words, wrongWords = true))
}

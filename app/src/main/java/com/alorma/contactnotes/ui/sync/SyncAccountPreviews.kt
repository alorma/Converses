package com.alorma.contactnotes.ui.sync

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

private val PreviewWords =
  listOf(
    "orbit",
    "maple",
    "velvet",
    "canyon",
    "ember",
    "piano",
    "lunar",
    "thistle",
    "harbor",
    "cobalt",
    "meadow",
    "quiver",
  )
private val PreviewAccount = SyncAccount(uid = "preview", name = "Bernat", email = "nom@gmail.com")
private const val PreviewNow = 1_700_000_000_000L
private const val FourMonthsMillis = 4L * 30 * 24 * 60 * 60 * 1000

@Preview
@Composable
internal fun SyncSignedOutPreview() {
  ConversesPreviewTheme {
    SyncScaffold(onBack = {}) { padding -> SyncSignedOutContent(padding = padding, onSignIn = {}) }
  }
}

@Preview
@Composable
internal fun SyncActivatingPreview() {
  ConversesPreviewTheme {
    SyncScaffold(onBack = {}) { padding ->
      SyncActivatingContent(padding = padding, takePrompt = { false }, onUnlocked = {}, onCancel = {})
    }
  }
}

@Composable
private fun SyncReadyPreview(wordsReviewedAt: Long?) {
  ConversesPreviewTheme {
    SyncScaffold(onBack = {}) { padding ->
      SyncReadyContent(
        account = PreviewAccount,
        wordsReviewedAt = wordsReviewedAt,
        padding = padding,
        onWordsUnlocked = {},
        onSignOut = {},
        now = PreviewNow,
      )
    }
  }
}

@Preview
@Composable
internal fun SyncReadyPreview() {
  SyncReadyPreview(wordsReviewedAt = PreviewNow - FourMonthsMillis)
}

@Preview
@Composable
internal fun SyncReadyNeverReviewedPreview() {
  SyncReadyPreview(wordsReviewedAt = null)
}

@Composable
private fun SecurityWordsPreview(activation: Boolean) {
  ConversesPreviewTheme {
    SecurityWordsContent(
      activation = activation,
      state = SecurityWordsState(words = PreviewWords, saved = activation),
      snackbar = remember { SnackbarHostState() },
      onClose = {},
      onCopy = {},
      onSavedChange = {},
      onContinue = {},
    )
  }
}

@Preview
@Composable
internal fun SecurityWordsActivationPreview() {
  SecurityWordsPreview(activation = true)
}

@Preview
@Composable
internal fun SecurityWordsReviewPreview() {
  SecurityWordsPreview(activation = false)
}

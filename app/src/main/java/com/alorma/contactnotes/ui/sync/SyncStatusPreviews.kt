package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.data.sync.SyncStatus
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewSurface
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

private val StatusAccount = SyncAccount(uid = "preview", name = "Bernat", email = "nom@gmail.com")
private const val StatusNow = 1_700_000_000_000L

@Composable
private fun ReadyWithStatus(status: SyncStatus) {
  ConversesPreviewTheme {
    SyncScaffold(onBack = {}) { padding ->
      SyncReadyContent(
        account = StatusAccount,
        wordsReviewedAt = null,
        status = status,
        padding = padding,
        onWordsUnlocked = {},
        onSignOut = {},
        now = StatusNow,
      )
    }
  }
}

@Preview
@Composable
internal fun SyncReadySyncingPreview() = ReadyWithStatus(SyncStatus.Syncing(null))

@Preview
@Composable
internal fun SyncReadyOfflinePreview() = ReadyWithStatus(SyncStatus.Offline(3))

@Preview
@Composable
internal fun SyncReadyFailedPreview() = ReadyWithStatus(SyncStatus.Failed)

/** Els estats de dades de l'indicador (13a): sincronitzant, sense connexió i error. */
@Preview
@Composable
internal fun SyncIndicatorDataStatesPreview() {
  ConversesPreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SyncIndicator(state = SyncIndicatorState.On("B", SyncIndicatorStatus.Syncing), onClick = {})
      SyncIndicator(state = SyncIndicatorState.On("B", SyncIndicatorStatus.Offline), onClick = {})
    }
  }
}

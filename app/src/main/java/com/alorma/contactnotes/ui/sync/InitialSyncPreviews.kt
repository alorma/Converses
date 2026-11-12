package com.alorma.contactnotes.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Composable
private fun InitialSyncPreview(state: InitialSyncUi) {
  ConversesPreviewTheme {
    InitialSyncContent(state = state, onHide = {}, onDone = {}, onMergeContinue = {})
  }
}

@Preview
@Composable
internal fun SyncUploadingPreview() = InitialSyncPreview(InitialSyncUi.Uploading(progress = 0.42f))

@Preview
@Composable
internal fun SyncDownloadingPreview() = InitialSyncPreview(InitialSyncUi.Downloading(progress = 0.7f))

@Preview
@Composable
internal fun SyncDoneInitialPreview() = InitialSyncPreview(InitialSyncUi.Done(deviceName = "Pixel 8"))

@Preview
@Composable
internal fun SyncMergePromptPreview() = InitialSyncPreview(InitialSyncUi.MergePrompt(entryCount = 12))

@Preview
@Composable
internal fun SyncInitialOfflinePreview() = InitialSyncPreview(InitialSyncUi.Offline)

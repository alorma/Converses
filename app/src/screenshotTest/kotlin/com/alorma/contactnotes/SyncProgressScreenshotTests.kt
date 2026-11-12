package com.alorma.contactnotes

import androidx.compose.runtime.Composable
import com.alorma.contactnotes.ui.sync.SyncDoneInitialPreview
import com.alorma.contactnotes.ui.sync.SyncDownloadingPreview
import com.alorma.contactnotes.ui.sync.SyncIndicatorDataStatesPreview
import com.alorma.contactnotes.ui.sync.SyncInitialOfflinePreview
import com.alorma.contactnotes.ui.sync.SyncMergePromptPreview
import com.alorma.contactnotes.ui.sync.SyncReadyFailedPreview
import com.alorma.contactnotes.ui.sync.SyncReadyOfflinePreview
import com.alorma.contactnotes.ui.sync.SyncReadyPreview
import com.alorma.contactnotes.ui.sync.SyncReadySyncingPreview
import com.alorma.contactnotes.ui.sync.SyncUploadingPreview
import com.android.tools.screenshot.PreviewTest

/** Pujada, baixada, fet i fusió (13g, 13m2, 13h, 13m), l'estat de 13o i els estats de dades de l'indicador (13a). */
class SyncProgressScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncUploading() {
    SyncUploadingPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncDownloading() {
    SyncDownloadingPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncInitialDone() {
    SyncDoneInitialPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncMergePrompt() {
    SyncMergePromptPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncInitialOffline() {
    SyncInitialOfflinePreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncReadyUpToDate() {
    SyncReadyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncReadySyncing() {
    SyncReadySyncingPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncReadyOffline() {
    SyncReadyOfflinePreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncReadyFailed() {
    SyncReadyFailedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncIndicatorDataStates() {
    SyncIndicatorDataStatesPreview()
  }
}

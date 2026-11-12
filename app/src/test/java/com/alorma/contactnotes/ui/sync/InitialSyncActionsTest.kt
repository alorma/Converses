package com.alorma.contactnotes.ui.sync

import com.alorma.contactnotes.data.sync.InitialSync
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialSyncActionsTest {
  private fun names(
    previous: InitialSync?,
    current: InitialSync?,
  ) = initialSyncActions(previous, current).map { it.eventName to it.eventProperties }

  @Test
  fun anUploadThatEndsIsCountedOnce() {
    assertEquals(emptyList<Any>(), names(InitialSync.Uploading(0.1f), InitialSync.Uploading(0.5f)))
    assertEquals(
      listOf("sync_upload_finished" to mapOf("result" to "done")),
      names(InitialSync.Uploading(0.5f), InitialSync.Done),
    )
  }

  @Test
  fun aDownloadThatGoesOfflineIsCounted() {
    assertEquals(
      listOf("sync_download_finished" to mapOf("result" to "offline")),
      names(InitialSync.Downloading(null), InitialSync.Offline),
    )
  }

  @Test
  fun inAMergeTheUploadEndsWhenTheDownloadStarts() {
    assertEquals(
      listOf("sync_upload_finished" to mapOf("result" to "done")),
      names(InitialSync.Uploading(1f), InitialSync.Downloading(null)),
    )
    assertEquals(emptyList<Any>(), names(InitialSync.Downloading(null), InitialSync.Downloading(0.5f)))
  }

  @Test
  fun theMergeDialogIsCountedWhenItAppears() {
    assertEquals(listOf("sync_merge_shown" to emptyMap<String, String>()), names(null, InitialSync.MergePrompt(3)))
    assertEquals(emptyList<Any>(), names(InitialSync.MergePrompt(3), InitialSync.MergePrompt(3)))
  }

  @Test
  fun hidingCountsNothing() {
    assertEquals(emptyList<Any>(), names(InitialSync.Uploading(0.5f), null))
  }
}

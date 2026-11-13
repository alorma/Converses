package com.alorma.contactnotes

import androidx.compose.runtime.Composable
import com.alorma.contactnotes.ui.people.PeopleListScreenSyncRemovedPreview
import com.alorma.contactnotes.ui.sync.SlotsPreview
import com.alorma.contactnotes.ui.sync.SyncDevicesPreview
import com.alorma.contactnotes.ui.sync.SyncIndicatorRemovedPreview
import com.alorma.contactnotes.ui.sync.SyncRemovedPreview
import com.android.tools.screenshot.PreviewTest

/** Dispositius i places (fase 5): 13l, 13o, 13o2, 13c i la insígnia de «tret». */
class SyncDevicesScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncSlotsFull() {
    SlotsPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncDevices() {
    SyncDevicesPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncRemoved() {
    SyncRemovedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun peopleListSyncRemoved() {
    PeopleListScreenSyncRemovedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncIndicatorRemoved() {
    SyncIndicatorRemovedPreview()
  }
}

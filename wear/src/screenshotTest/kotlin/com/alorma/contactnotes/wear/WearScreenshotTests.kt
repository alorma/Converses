package com.alorma.contactnotes.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.wear.ui.ApprovalDeniedPreview
import com.alorma.contactnotes.wear.ui.ApprovalExpiredPreview
import com.alorma.contactnotes.wear.ui.ApprovalNoPhonePreview
import com.alorma.contactnotes.wear.ui.ApprovalWaitingPreview
import com.alorma.contactnotes.wear.ui.CompanionMissingPreview
import com.alorma.contactnotes.wear.ui.CompanionOpenedPreview
import com.alorma.contactnotes.wear.ui.DictateErrorPreview
import com.alorma.contactnotes.wear.ui.DictateListeningPreview
import com.alorma.contactnotes.wear.ui.DictatePermissionPreview
import com.alorma.contactnotes.wear.ui.DictatePreview
import com.alorma.contactnotes.wear.ui.LockRequiredPreview
import com.alorma.contactnotes.wear.ui.NoDataPreview
import com.alorma.contactnotes.wear.ui.NoPeoplePreview
import com.alorma.contactnotes.wear.ui.PeopleDiscardedNotesPreview
import com.alorma.contactnotes.wear.ui.PeoplePhoneOfflinePreview
import com.alorma.contactnotes.wear.ui.PeoplePreview
import com.alorma.contactnotes.wear.ui.SavedPendingPreview
import com.alorma.contactnotes.wear.ui.SavedPreview
import com.alorma.contactnotes.wear.ui.TimelineEmptyPreview
import com.alorma.contactnotes.wear.ui.TimelineLoadingMorePreview
import com.alorma.contactnotes.wear.ui.TimelinePreview
import com.alorma.contactnotes.wear.ui.WhoPreview
import com.android.tools.screenshot.PreviewTest

/** Les 9 pantalles del brief, en català com les captures de referència, a Large i Small Round. */
@Preview(name = "Large Round", device = Devices.WEAR_OS_LARGE_ROUND, locale = "ca")
@Preview(name = "Small Round", device = Devices.WEAR_OS_SMALL_ROUND, locale = "ca")
annotation class WatchPreviews

class WearScreenshotTests {
  @WatchPreviews
  @PreviewTest
  @Composable
  fun people() = PeoplePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun timeline() = TimelinePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun timelineLoadingMore() = TimelineLoadingMorePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun timelineEmpty() = TimelineEmptyPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun noData() = NoDataPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun noPeople() = NoPeoplePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun dictate() = DictatePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun dictateListening() = DictateListeningPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun dictatePermission() = DictatePermissionPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun dictateError() = DictateErrorPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun who() = WhoPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun saved() = SavedPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun savedPending() = SavedPendingPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun approvalWaiting() = ApprovalWaitingPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun approvalDenied() = ApprovalDeniedPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun approvalExpired() = ApprovalExpiredPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun approvalNoPhone() = ApprovalNoPhonePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun peoplePhoneOffline() = PeoplePhoneOfflinePreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun peopleDiscardedNotes() = PeopleDiscardedNotesPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun lockRequired() = LockRequiredPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun companionMissing() = CompanionMissingPreview()

  @WatchPreviews
  @PreviewTest
  @Composable
  fun companionOpened() = CompanionOpenedPreview()
}

package com.alorma.contactnotes

import androidx.compose.runtime.Composable
import com.alorma.contactnotes.ui.sync.SecurityWordsActivationPreview
import com.alorma.contactnotes.ui.sync.SecurityWordsReviewPreview
import com.alorma.contactnotes.ui.sync.SyncActivatingPreview
import com.alorma.contactnotes.ui.sync.SyncReadyNeverReviewedPreview
import com.alorma.contactnotes.ui.sync.SyncReadyPreview
import com.alorma.contactnotes.ui.sync.SyncSignedOutPreview
import com.android.tools.screenshot.PreviewTest

/** Les pantalles del compte de sincronització (fase 3): sense sessió, empremta, paraules i amb sessió. */
class SyncAccountScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncSignedOut() {
    SyncSignedOutPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncActivating() {
    SyncActivatingPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncReady() {
    SyncReadyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncReadyNeverReviewed() {
    SyncReadyNeverReviewedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun securityWordsActivation() {
    SecurityWordsActivationPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun securityWordsReview() {
    SecurityWordsReviewPreview()
  }

  @TabletPortraitPreviews
  @PreviewTest
  @Composable
  fun syncSignedOutTablet() {
    SyncSignedOutPreview()
  }
}

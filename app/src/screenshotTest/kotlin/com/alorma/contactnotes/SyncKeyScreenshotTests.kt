package com.alorma.contactnotes

import androidx.compose.runtime.Composable
import com.alorma.contactnotes.ui.sync.EnterWordsEmptyPreview
import com.alorma.contactnotes.ui.sync.EnterWordsTypingPreview
import com.alorma.contactnotes.ui.sync.EnterWordsWrongPreview
import com.alorma.contactnotes.ui.sync.SyncNeedsKeyPreview
import com.android.tools.screenshot.PreviewTest

/** Sincronització: la clau en un dispositiu nou (13i, 13j, 13j2). */
class SyncKeyScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncNeedsKey() {
    SyncNeedsKeyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun enterWordsEmpty() {
    EnterWordsEmptyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun enterWordsTyping() {
    EnterWordsTypingPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun enterWordsWrong() {
    EnterWordsWrongPreview()
  }
}

package com.alorma.contactnotes.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Preview
@Composable
internal fun AppLockPreview() {
  ConversesPreviewTheme {
    AppLockContent(canUnlock = true, onUnlock = {})
  }
}

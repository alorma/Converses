package com.alorma.contactnotes.ui.watchinstall

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Preview
@Composable
internal fun WatchInstallProposePreview() {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      WatchInstallContent(
        state = WatchInstallUiState.Propose(watchName = "Pixel Watch 3"),
        onInstall = {},
        onNotNow = {},
        onNeverAgain = {},
        onDone = {},
      )
    }
  }
}

@Preview
@Composable
internal fun WatchInstallFinishPreview() {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      WatchInstallContent(
        state = WatchInstallUiState.Finish(watchName = "Pixel Watch 3"),
        onInstall = {},
        onNotNow = {},
        onNeverAgain = {},
        onDone = {},
      )
    }
  }
}

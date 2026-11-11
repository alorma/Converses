package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewSurface

/** Els tres estats d'aquesta fase (13a): sense sessió, amb sessió i «falten les paraules». */
@Preview
@Composable
internal fun SyncIndicatorStatesPreview() {
  ConversesPreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SyncIndicator(state = SyncIndicatorState.SignedOut, onClick = {})
      SyncIndicator(state = SyncIndicatorState.On("B"), onClick = {})
      SyncIndicator(state = SyncIndicatorState.NeedsKey("B"), onClick = {})
    }
  }
}

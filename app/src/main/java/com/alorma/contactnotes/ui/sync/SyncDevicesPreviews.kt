package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.data.sync.DeviceInfo
import com.alorma.contactnotes.data.sync.SlotsPrompt
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.data.sync.SyncStatus
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewSurface
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

private const val Now = 1_700_000_000_000L
private const val MinutesMillis = 60 * 1000L
private const val HoursMillis = 60 * MinutesMillis
private val Account = SyncAccount(uid = "preview", name = "Bernat", email = "nom@gmail.com")

private val ThisDevice = DeviceInfo("a", "Pixel 8", Now - 3_000_000, Now - 20_000, isThis = true)
private val OtherDevice = DeviceInfo("b", "Galaxy Tab S9", Now - 9_000_000, Now - 3 * HoursMillis, isThis = false)

@Composable
private fun DevicesPreviewBody(
  removed: Boolean,
  devices: List<DeviceInfo>,
) {
  ConversesPreviewTheme {
    SyncScaffold(onBack = {}) { padding ->
      SyncReadyContent(
        account = Account,
        wordsReviewedAt = Now - 4L * 30 * 24 * HoursMillis,
        status = SyncStatus.UpToDate(Now - 2 * MinutesMillis),
        padding = padding,
        onWordsUnlocked = {},
        onSignOut = {},
        devices = devices,
        deviceLimit = 2,
        removed = removed,
        now = Now,
      )
    }
  }
}

/** 13o: «Dispositius» amb «2 de 2», aquest dispositiu primer i la fila de Pro. */
@Preview
@Composable
internal fun SyncDevicesPreview() {
  DevicesPreviewBody(removed = false, devices = listOf(ThisDevice, OtherDevice))
}

/** 13o2: l'avís `err-c` en lloc de l'estat, i els dispositius que sí que sincronitzen. */
@Preview
@Composable
internal fun SyncRemovedPreview() {
  DevicesPreviewBody(
    removed = true,
    devices = listOf(OtherDevice, DeviceInfo("c", null, Now - 9_000_000, Now - 5 * MinutesMillis, isThis = false)),
  )
}

/** 13l: res triat; «Treu-lo i continua» apagat. */
@Preview
@Composable
internal fun SlotsPreview() {
  ConversesPreviewTheme {
    SlotsContent(
      prompt =
        SlotsPrompt(
          limit = 2,
          devices =
            listOf(
              DeviceInfo("a", "Pixel 7", Now - 9_000_000, Now - 5 * HoursMillis, isThis = false),
              OtherDevice.copy(lastSyncAt = Now - 21 * 24 * HoursMillis),
            ),
          canUpgrade = true,
        ),
      selectedId = null,
      working = false,
      snackbar = remember { SnackbarHostState() },
      onSelect = {},
      onRemoveAndContinue = {},
      onNotNow = {},
      onOpenPro = {},
      now = Now,
    )
  }
}

/** Dispositiu tret (13a): la insígnia en `err`, al costat de les altres. */
@Preview
@Composable
internal fun SyncIndicatorRemovedPreview() {
  ConversesPreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SyncIndicator(state = SyncIndicatorState.Removed("B"), onClick = {})
    }
  }
}

package com.alorma.contactnotes.ui.sync

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.SyncStatus

/** L'estat de les dades a 13o: `cloud_done` «Al dia · fa N minuts», «Sincronitzant…», «Sense connexió…» o l'error. */
@Composable
fun SyncStatusRow(
  status: SyncStatus,
  now: Long,
  modifier: Modifier = Modifier,
) {
  val icon =
    when (status) {
      is SyncStatus.UpToDate -> Icons.Rounded.CloudDone
      is SyncStatus.Syncing -> Icons.Rounded.Sync
      is SyncStatus.Offline -> Icons.Rounded.CloudOff
      SyncStatus.Failed -> Icons.Rounded.SyncProblem
    }
  val text =
    when (status) {
      is SyncStatus.UpToDate -> upToDateLabel(status.at, now)
      is SyncStatus.Syncing -> stringResource(R.string.sync_st_sync)
      is SyncStatus.Offline -> pluralStringResource(R.plurals.sync_st_pend, status.pending, status.pending)
      SyncStatus.Failed -> stringResource(R.string.sync_st_failed)
    }
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun upToDateLabel(
  at: Long?,
  now: Long,
): String =
  if (at == null || now - at < DateUtils.MINUTE_IN_MILLIS) {
    stringResource(R.string.sync_st_now)
  } else {
    val relative = DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString()
    stringResource(R.string.sync_st_ok, relative)
  }

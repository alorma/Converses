package com.alorma.contactnotes.ui.sync

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.data.sync.SyncStatus
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.SettingRow
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.promptUnlock

/**
 * Amb sessió i clau (13o, reduïda: sense l'estat, els dispositius ni esborrar el compte). La fila de les paraules
 * demana l'empremta i, si va bé, crida [onWordsUnlocked]; [onSignOut] es crida després de la confirmació.
 * [now] només es fixa a les previsualitzacions.
 */
@Composable
fun SyncReadyContent(
  account: SyncAccount,
  wordsReviewedAt: Long?,
  status: SyncStatus,
  padding: PaddingValues,
  onWordsUnlocked: () -> Unit,
  onSignOut: () -> Unit,
  now: Long = remember { System.currentTimeMillis() },
) {
  val activity = LocalActivity.current as? FragmentActivity
  val inspection = LocalInspectionMode.current
  val bioTitle = stringResource(R.string.sync_bio_title)
  val bioSubtitle = stringResource(R.string.sync_bio_words)
  var confirmSignOut by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(padding)
        .readableWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    AccountCard(account)
    SyncStatusRow(status = status, now = now)
    Section(title = stringResource(R.string.sync_security)) {
      SettingRow(
        title = stringResource(R.string.sync_words_title),
        subtitle = wordsReviewedAt?.let { reviewedAgo(it, now).label() },
        shape = groupedItemShape(0, 1),
        onClick = {
          if (activity != null && !inspection) {
            if (activity.canUnlockWithDevice()) {
              activity.promptUnlock(bioTitle, bioSubtitle) { onWordsUnlocked() }
            } else {
              onWordsUnlocked()
            }
          }
        },
        trailing = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
      )
    }
    Section(title = stringResource(R.string.sync_account)) {
      SettingRow(
        title = stringResource(R.string.sync_sign_out),
        subtitle = stringResource(R.string.sync_sign_out_summary),
        icon = Icons.AutoMirrored.Rounded.Logout,
        shape = groupedItemShape(0, 1),
        onClick = { confirmSignOut = true },
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        Icons.Rounded.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = stringResource(R.string.sync_e2e),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  if (confirmSignOut) {
    AlertDialog(
      onDismissRequest = { confirmSignOut = false },
      title = { Text(stringResource(R.string.sync_sign_out_confirm_title)) },
      text = { Text(stringResource(R.string.sync_sign_out_confirm_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            confirmSignOut = false
            onSignOut()
          },
        ) { Text(stringResource(R.string.sync_sign_out)) }
      },
      dismissButton = {
        TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.sync_cancel)) }
      },
    )
  }
}

@Composable
private fun AccountCard(account: SyncAccount) {
  val initial =
    (account.name?.firstOrNull { it.isLetterOrDigit() } ?: account.email?.firstOrNull { it.isLetterOrDigit() })
      ?.uppercaseChar()
      ?.toString()
      .orEmpty()
  Surface(shape = groupedItemShape(0, 1), color = GroupedListDefaults.containerColor) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initial,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontSize = 20.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        account.name?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
        account.email?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun Section(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      modifier = Modifier.padding(horizontal = 6.dp),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    content()
  }
}

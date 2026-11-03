package com.alorma.contactnotes.ui.entry

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.openSecuritySetup
import com.alorma.contactnotes.ui.person.promptUnlock
import org.koin.compose.koinInject

/** Amb una persona protegida triada (o a punt de ser-ho) la pantalla no es pot capturar ni surt a Recents. */
@Composable
fun SecureWhile(secure: Boolean) {
  val secureWindow: SecureWindow = koinInject()
  DisposableEffect(secure) {
    if (secure) secureWindow.acquire()
    onDispose { if (secure) secureWindow.release() }
  }
}

/**
 * Demana desbloquejar [pending] (una persona protegida triada en un selector); sense bloqueig de pantalla,
 * mostra el diàleg per configurar-lo. [onCancel] deixa la selecció com estava.
 */
@Composable
fun PendingPersonUnlock(
  pending: PersonEntity?,
  onUnlock: (personId: Long) -> Unit,
  onCancel: () -> Unit,
) {
  val activity = LocalActivity.current as FragmentActivity
  var showNoLock by remember { mutableStateOf(false) }
  val authTitle = stringResource(R.string.unlock_title, pending?.name.orEmpty())
  val authSubtitle = stringResource(R.string.unlock_subtitle)
  LaunchedEffect(pending?.id) {
    if (pending != null) {
      if (activity.canUnlockWithDevice()) {
        activity.promptUnlock(authTitle, authSubtitle, onError = { onCancel() }) { onUnlock(pending.id) }
      } else {
        showNoLock = true
      }
    }
  }

  if (showNoLock) {
    val dismiss = {
      showNoLock = false
      onCancel()
    }
    AlertDialog(
      onDismissRequest = dismiss,
      title = { Text(stringResource(R.string.lock_none_title)) },
      text = { Text(stringResource(R.string.lock_none_body)) },
      confirmButton = {
        TextButton(onClick = { activity.openSecuritySetup() }) { Text(stringResource(R.string.lock_setup)) }
      },
      dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
  }
}

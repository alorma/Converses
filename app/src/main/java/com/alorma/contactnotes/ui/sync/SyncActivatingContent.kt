package com.alorma.contactnotes.ui.sync

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.alorma.contactnotes.R
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.promptUnlock

/**
 * Primer dispositiu, amb la sessió iniciada i abans de veure les paraules (13e): en aparèixer, demana l'empremta si
 * [takePrompt] ho diu, i si va bé crida [onUnlocked]. El ViewModel en porta el compte, una vegada per activació:
 * girar la pantalla no l'ha de tornar a obrir, i un segon inici de sessió sí. Cancel·lar-la o qualsevol error crida
 * [onCancel], que tanca la sessió. Si el dispositiu no té empremta ni bloqueig de pantalla no hi ha res a demanar i es continua directament.
 */
@Composable
fun SyncActivatingContent(
  padding: PaddingValues,
  takePrompt: () -> Boolean,
  onUnlocked: () -> Unit,
  onCancel: () -> Unit,
) {
  val activity = LocalActivity.current as? FragmentActivity
  val inspection = LocalInspectionMode.current
  val title = stringResource(R.string.sync_bio_title)
  val subtitle = stringResource(R.string.sync_bio_words)
  val currentOnUnlocked by rememberUpdatedState(onUnlocked)
  val currentOnCancel by rememberUpdatedState(onCancel)
  LaunchedEffect(Unit) {
    if (inspection || activity == null || !takePrompt()) return@LaunchedEffect
    if (activity.canUnlockWithDevice()) {
      activity.promptUnlock(title, subtitle, onError = { currentOnCancel() }) { currentOnUnlocked() }
    } else {
      currentOnUnlocked()
    }
  }
  Column(
    modifier = Modifier.fillMaxSize().padding(padding),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
    // Una sortida si el diàleg del sistema s'ha perdut (p. ex. en girar la pantalla)
    TextButton(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
      Text(stringResource(R.string.sync_cancel))
    }
  }
}

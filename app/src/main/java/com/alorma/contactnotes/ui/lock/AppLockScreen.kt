package com.alorma.contactnotes.ui.lock

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.promptUnlock

/** Pantalla que tapa tota l'app mentre el bloqueig és actiu; demana l'empremta o el PIN en aparèixer. */
@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
  val activity = LocalActivity.current as FragmentActivity
  val canUnlock = remember { activity.canUnlockWithDevice() }
  val currentOnUnlocked by rememberUpdatedState(onUnlocked)
  var autoLaunched by rememberSaveable { mutableStateOf(false) }
  val title = stringResource(R.string.app_lock_prompt_title)
  val subtitle = stringResource(R.string.unlock_subtitle)
  val unlock = { activity.promptUnlock(title, subtitle) { currentOnUnlocked() } }

  LaunchedEffect(canUnlock) {
    if (canUnlock && !autoLaunched) {
      autoLaunched = true
      unlock()
    }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(56.dp))
      Spacer(Modifier.height(16.dp))
      Text(stringResource(R.string.app_lock_title), style = ScreenTitleStyle, textAlign = TextAlign.Center)
      Spacer(Modifier.height(32.dp))
      Button(onClick = { unlock() }, enabled = canUnlock) {
        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(stringResource(R.string.unlock_action))
      }
    }
  }
}

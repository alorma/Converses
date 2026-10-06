package com.alorma.contactnotes.ui.person

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import com.alorma.contactnotes.ui.people.initialsOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PersonLockedScreen(
  personId: Long,
  onBack: () -> Unit,
  onUnlocked: () -> Unit,
  viewModel: PersonViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val person by viewModel.person.collectAsStateWithLifecycle()
  val activity = LocalActivity.current as FragmentActivity
  var canUnlock by remember { mutableStateOf(activity.canUnlockWithDevice()) }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canUnlock = activity.canUnlockWithDevice() }
  val currentOnUnlocked by rememberUpdatedState(onUnlocked)
  var autoLaunched by rememberSaveable { mutableStateOf(false) }

  val name = person?.name
  val title = stringResource(R.string.unlock_title, name.orEmpty())
  val subtitle = stringResource(R.string.unlock_subtitle)
  val unlock = { activity.promptUnlock(title, subtitle) { currentOnUnlocked() } }

  LaunchedEffect(name, canUnlock) {
    if (name != null && canUnlock && !autoLaunched) {
      autoLaunched = true
      unlock()
    }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
      IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
      }
      person?.let { p ->
        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Avatar(
            initials = initialsOf(p.name),
            shape = p.avatarShape,
            color = p.avatarColor,
            size = 112.dp,
            locked = true,
          )
          Spacer(Modifier.height(24.dp))
          Text(text = p.name, style = PersonNameStyle, textAlign = TextAlign.Center)
          Spacer(Modifier.height(12.dp))
          Text(
            text = stringResource(if (canUnlock) R.string.locked_description else R.string.locked_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(32.dp))
          Button(onClick = { if (canUnlock) unlock() else activity.openSecuritySetup() }) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(stringResource(if (canUnlock) R.string.unlock_action else R.string.settings_security_setup))
          }
        }
      }
    }
  }
}

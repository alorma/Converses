package com.alorma.contactnotes.ui.person

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NoEncryption
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ConversesLoadingIndicator
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.ui.people.initialsOf
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Alçada de l'estat més alt (missatge i dos botons): així l'avatar i el nom no es mouen entre estats.
private val StateAreaMinHeight = 288.dp

/**
 * Pantalla de bloqueig d'una persona protegida. Només rep el nom, la forma i el color de l'avatar:
 * res de la persona es carrega fins que el desbloqueig va bé i la ruta passa al detall.
 */
@Composable
fun PersonLockedScreen(
  personId: Long,
  onBack: () -> Unit,
  onUnlocked: () -> Unit,
  viewModel: PersonViewModel = koinViewModel(parameters = { parametersOf(personId) }),
  lockViewModel: PersonLockViewModel = koinViewModel(),
) {
  val person by viewModel.person.collectAsStateWithLifecycle()
  val state by lockViewModel.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current as FragmentActivity
  val currentOnUnlocked by rememberUpdatedState(onUnlocked)

  val secureWindow: SecureWindow = koinInject()
  DisposableEffect(Unit) {
    secureWindow.acquire()
    onDispose { secureWindow.release() }
  }

  val name = person?.name
  val title = stringResource(R.string.unlock_title, name.orEmpty())
  val subtitle = stringResource(R.string.unlock_subtitle)
  val begin = {
    if (lockViewModel.begin(activity.unlockAvailability())) {
      activity.promptUnlock(title, subtitle, onError = lockViewModel::onError) { currentOnUnlocked() }
    }
  }

  LaunchedEffect(name) {
    if (name != null && !lockViewModel.started) begin()
  }
  // En tornar de la configuració de seguretat es torna a comprovar; la resta d'estats no reobren el prompt.
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (lockViewModel.started && state == LockState.NoCredentials) begin()
  }

  PersonTheme(person?.color) {
    person?.let { p ->
      PersonLockedContent(
        name = p.name,
        shape = p.shape,
        color = p.color,
        state = state,
        onBack = onBack,
        onRetry = { begin() },
        onSetup = { activity.openSecuritySetup() },
      )
    } ?: Scaffold(containerColor = MaterialTheme.colorScheme.surface) {}
  }
}

@Composable
fun PersonLockedContent(
  name: String,
  shape: AvatarShape,
  color: AvatarColor,
  state: LockState,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onSetup: () -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      IconButton(
        onClick = onBack,
        modifier =
          Modifier
            .windowInsetsPadding(
              WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
              ),
            ).padding(8.dp),
      ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
      }
    },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Avatar(initials = initialsOf(name), shape = shape, color = color, size = 112.dp, locked = true)
        Spacer(Modifier.height(24.dp))
        Text(text = name, style = PersonNameStyle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(
          modifier = Modifier.fillMaxWidth().heightIn(min = StateAreaMinHeight),
          contentAlignment = Alignment.TopCenter,
        ) {
          AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "lockState",
          ) { current ->
            LockStateContent(current, name, onBack, onRetry, onSetup)
          }
        }
      }
    }
  }
}

@Composable
private fun LockStateContent(
  state: LockState,
  name: String,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onSetup: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
    when (state) {
      LockState.Waiting -> {
        ConversesLoadingIndicator(size = 56.dp)
        Spacer(Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.lock_waiting),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      is LockState.Canceled -> {
        Message(
          icon = Icons.Rounded.Info,
          title = stringResource(R.string.lock_canceled_title),
          body =
            if (state.permanentLockout) {
              stringResource(R.string.lock_lockout_permanent)
            } else {
              stringResource(R.string.lock_canceled_body, name)
            },
        )
        Actions(
          primaryIcon = Icons.Rounded.Fingerprint,
          primaryLabel = stringResource(R.string.lock_retry),
          onPrimary = onRetry,
          onBack = onBack,
        )
      }

      is LockState.LockedOut -> {
        Message(
          icon = Icons.Rounded.Error,
          title = stringResource(R.string.lock_lockout_title),
          body = stringResource(R.string.lock_lockout_body),
          error = true,
        )
        Actions(
          primaryIcon = Icons.Rounded.Timer,
          primaryLabel =
            if (state.seconds > 0) {
              stringResource(R.string.lock_retry_in, state.seconds)
            } else {
              stringResource(R.string.lock_retry)
            },
          primaryEnabled = state.seconds == 0,
          onPrimary = onRetry,
          onBack = onBack,
        )
      }

      LockState.NoCredentials -> {
        Message(
          icon = Icons.Rounded.NoEncryption,
          title = stringResource(R.string.lock_none_title),
          body = stringResource(R.string.lock_none_body),
        )
        Actions(
          primaryIcon = Icons.AutoMirrored.Rounded.OpenInNew,
          primaryLabel = stringResource(R.string.lock_setup),
          onPrimary = onSetup,
          onBack = onBack,
        )
      }
    }
  }
}

@Composable
private fun Message(
  icon: ImageVector,
  title: String,
  body: String,
  error: Boolean = false,
) {
  val scheme = MaterialTheme.colorScheme
  Icon(icon, contentDescription = null, tint = if (error) scheme.error else scheme.onSurfaceVariant)
  Spacer(Modifier.height(8.dp))
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = if (error) scheme.error else scheme.onSurface,
    textAlign = TextAlign.Center,
  )
  Spacer(Modifier.height(4.dp))
  Text(
    text = body,
    style = MaterialTheme.typography.bodyLarge,
    color = scheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
  )
  Spacer(Modifier.height(24.dp))
}

@Composable
private fun Actions(
  primaryIcon: ImageVector,
  primaryLabel: String,
  onPrimary: () -> Unit,
  onBack: () -> Unit,
  primaryEnabled: Boolean = true,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onPrimary, enabled = primaryEnabled, modifier = Modifier.fillMaxWidth().height(56.dp)) {
      Icon(primaryIcon, contentDescription = null)
      Spacer(Modifier.padding(horizontal = 4.dp))
      Text(primaryLabel)
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
      Text(stringResource(R.string.lock_back))
    }
  }
}

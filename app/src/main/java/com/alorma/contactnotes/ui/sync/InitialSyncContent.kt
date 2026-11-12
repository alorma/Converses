package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import com.alorma.contactnotes.shared.AvatarShape

/**
 * El que ensenyen la pujada (13g), la baixada (13m2), el «fet» (13h) i el diàleg de fusió (13m). Mai noms ni contingut:
 * només recomptes i el nom del dispositiu. [progress] va de 0 a 1; `null` és una barra indeterminada.
 */
sealed interface InitialSyncUi {
  data class Uploading(
    val progress: Float?,
  ) : InitialSyncUi

  data class Downloading(
    val progress: Float?,
  ) : InitialSyncUi

  /** Diàleg abans de baixar, quan el dispositiu ja té entrades pròpies. */
  data class MergePrompt(
    val entryCount: Int,
  ) : InitialSyncUi

  data class Done(
    val deviceName: String,
  ) : InitialSyncUi

  data object Offline : InitialSyncUi

  data object Failed : InitialSyncUi
}

/**
 * Pantalla de pujada, baixada, fet o error d'una sincronització completa. «Amaga» ([onHide]) la tanca i la feina
 * continua en segon pla, sense notificació; [onDone] és el «Fet» de [InitialSyncUi.Done]; [onMergeContinue] és el
 * «Continua» de [InitialSyncUi.MergePrompt] (unió: no es perd res).
 */
@Composable
fun InitialSyncContent(
  state: InitialSyncUi,
  onHide: () -> Unit,
  onDone: () -> Unit,
  onMergeContinue: () -> Unit,
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = { ConversesTopBar(title = stringResource(R.string.sync_title), scrollBehavior = scrollBehavior) },
  ) { padding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(padding)
          .readableWidth()
          .padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when (state) {
        is InitialSyncUi.Uploading ->
          ProgressBody(Icons.Rounded.CloudUpload, R.string.sync_up_t, state.progress, onHide)
        is InitialSyncUi.Downloading ->
          ProgressBody(Icons.Rounded.CloudDownload, R.string.sync_down_t, state.progress, onHide)
        // Darrere del diàleg, la baixada que començarà en continuar
        is InitialSyncUi.MergePrompt -> ProgressBody(Icons.Rounded.CloudDownload, R.string.sync_down_t, null, onHide)
        is InitialSyncUi.Done -> DoneBody(state.deviceName, onDone)
        InitialSyncUi.Offline -> ErrorBody(R.string.sync_error_offline, onHide)
        InitialSyncUi.Failed -> ErrorBody(R.string.sync_error_failed, onHide)
      }
    }
  }
  if (state is InitialSyncUi.MergePrompt) {
    AlertDialog(
      onDismissRequest = {},
      properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
      title = { Text(stringResource(R.string.sync_merge_t)) },
      text = { Text(pluralStringResource(R.plurals.sync_merge_b, state.entryCount, state.entryCount)) },
      confirmButton = { TextButton(onClick = onMergeContinue) { Text(stringResource(R.string.sync_continue)) } },
    )
  }
}

@Composable
private fun ProgressBody(
  icon: ImageVector,
  title: Int,
  progress: Float?,
  onHide: () -> Unit,
) {
  Cookie(
    icon = icon,
    container = MaterialTheme.colorScheme.primaryContainer,
    content = MaterialTheme.colorScheme.onPrimaryContainer,
  )
  Title(stringResource(title))
  val bar = Modifier.fillMaxWidth().height(8.dp)
  if (progress == null) {
    LinearProgressIndicator(modifier = bar)
  } else {
    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = bar, drawStopIndicator = {})
  }
  HideButton(onHide)
}

@Composable
private fun DoneBody(
  deviceName: String,
  onDone: () -> Unit,
) {
  Cookie(
    icon = Icons.Rounded.Check,
    container = MaterialTheme.colorScheme.primary,
    content = MaterialTheme.colorScheme.onPrimary,
  )
  Title(stringResource(R.string.sync_done_t))
  Text(
    text = stringResource(R.string.sync_done_s, deviceName),
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
  )
  Button(onClick = onDone, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.sync_done)) }
}

@Composable
private fun ErrorBody(
  message: Int,
  onHide: () -> Unit,
) {
  Cookie(
    icon = Icons.Rounded.CloudOff,
    container = MaterialTheme.colorScheme.surfaceContainerHighest,
    content = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Title(stringResource(message))
  HideButton(onHide)
}

@Composable
private fun HideButton(onHide: () -> Unit) {
  TextButton(onClick = onHide) { Text(stringResource(R.string.sync_hide)) }
}

@Composable
private fun Title(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )
}

/** Galeta de 88 dp. */
@Composable
private fun Cookie(
  icon: ImageVector,
  container: Color,
  content: Color,
) {
  Box(
    modifier = Modifier.size(88.dp).clip(AvatarShape.Cookie9Sided.asShape()).background(container),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(40.dp))
  }
}

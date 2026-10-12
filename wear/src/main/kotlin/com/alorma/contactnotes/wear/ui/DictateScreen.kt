package com.alorma.contactnotes.wear.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.data.SpeechError
import com.alorma.contactnotes.wear.theme.ButtonLabelStyle
import com.alorma.contactnotes.wear.theme.DictationTextStyle
import com.alorma.contactnotes.wear.theme.OverlineStyle
import com.alorma.contactnotes.wear.theme.PersonNameStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Alçades i colors de les ones del brief (8 barres; les del mig, més clares i altes). */
private val VoiceBars =
  listOf(
    12 to WearColors.Outline,
    20 to WearColors.OnSurfaceVariant,
    28 to WearColors.Primary,
    32 to WearColors.Primary,
    24 to WearColors.Primary,
    30 to WearColors.Primary,
    18 to WearColors.OnSurfaceVariant,
    12 to WearColors.Outline,
  )

/** Les barres no baixen mai d'aquesta fracció de la seva alçada, perquè en silenci es vegin igualment. */
private const val MinBarFraction = 0.45f

/**
 * Pantalla 3 amb el reconeixedor: demana el permís del micròfon, escolta i mostra la transcripció en directe. En
 * acabar es queda amb el text definitiu fins que l'usuari toca «Acaba» ([onDone], que passa el text a «Qui»).
 */
@Composable
fun DictateScreen(
  viewModel: DictationViewModel,
  personName: String,
  onDone: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var granted by remember { mutableStateOf(context.hasMicrophonePermission()) }
  var asked by rememberSaveable { mutableStateOf(false) }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(granted) {
    if (granted) {
      viewModel.start()
    } else if (!asked) {
      asked = true
      launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }
  // Mai amb el micròfon obert en segon pla
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.finish() }
  // Si l'usuari torna dels ajustos amb el permís donat
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = context.hasMicrophonePermission() }

  // Denegat dues vegades el sistema ja no mostra el diàleg: només queden els ajustos de l'app
  val onAllow = {
    val activity = context as? Activity
    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
      context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
      )
    } else {
      launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  val current = state
  when {
    !granted ->
      DictatePermissionContent(onAllow = onAllow, modifier = modifier)
    current is DictationUiState.Failed ->
      DictateErrorContent(reason = current.reason, onRetry = viewModel::retry, modifier = modifier)
    current is DictationUiState.Done ->
      DictateContent(
        personName = personName,
        text = current.text,
        level = null,
        onFinish = { onDone(current.text) },
        modifier = modifier,
      )
    current is DictationUiState.Listening ->
      DictateContent(
        personName = personName,
        text = current.text,
        level = current.level,
        onFinish = viewModel::finish,
        modifier = modifier,
      )
  }
}

/** Pantalla 3: la transcripció, les ones i «Acaba». Sense [level] les ones es queden quietes. */
@Composable
fun DictateContent(
  personName: String,
  text: String,
  onFinish: () -> Unit,
  modifier: Modifier = Modifier,
  level: Float? = null,
) {
  ScreenScaffold(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(start = 30.dp, end = 30.dp, top = 38.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.note_for, personName).uppercase(),
        style = OverlineStyle,
        color = WearColors.OnSurfaceVariant,
      )
      Text(text = text, style = DictationTextStyle, color = WearColors.OnSurface, textAlign = TextAlign.Center)
    }
    VoiceWaves(level = level, modifier = Modifier.align(Alignment.Center).padding(top = 56.dp))
    PinnedEdgeButton(onClick = onFinish) {
      Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
      Text(text = stringResource(R.string.dictate_finish), style = ButtonLabelStyle)
    }
  }
}

/** Micròfon denegat (P4): «Permet» torna a demanar el permís o, si el sistema ja no el mostra, obre els ajustos. */
@Composable
fun DictatePermissionContent(
  onAllow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  StatusContent(
    icon = Icons.Rounded.Mic,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(R.string.dictate_permission_title),
    subtitle = stringResource(R.string.dictate_permission_hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
    action = stringResource(R.string.dictate_permission_action),
    onAction = onAllow,
  )
}

/** Error del reconeixedor (P4). Si el rellotge no sap reconèixer veu no s'ofereix tornar-ho a provar. */
@Composable
fun DictateErrorContent(
  reason: SpeechError,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (title, hint) =
    when (reason) {
      SpeechError.NoSpeech -> R.string.dictate_error_no_speech_title to R.string.dictate_error_no_speech_hint
      SpeechError.Unavailable -> R.string.dictate_error_unavailable_title to R.string.dictate_error_unavailable_hint
      SpeechError.Microphone -> R.string.dictate_error_microphone_title to R.string.dictate_error_microphone_hint
      SpeechError.Failed -> R.string.dictate_error_failed_title to R.string.dictate_error_failed_hint
    }
  val canRetry = reason != SpeechError.Unavailable
  StatusContent(
    icon = Icons.Rounded.MicOff,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(title),
    subtitle = stringResource(hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
    action = if (canRetry) stringResource(R.string.dictate_retry) else null,
    onAction = onRetry,
  )
}

@Composable
private fun VoiceWaves(
  level: Float?,
  modifier: Modifier = Modifier,
) {
  val animated = level != null && rememberAnimationsOn()
  val fraction by animateFloatAsState(
    targetValue = if (animated) MinBarFraction + (1f - MinBarFraction) * (level ?: 0f) else 1f,
    label = "voiceLevel",
  )
  val description = stringResource(R.string.dictate_listening)
  Row(
    modifier = modifier.height(32.dp).semantics { contentDescription = description },
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    VoiceBars.forEach { (height, color) -> Bar(height = height * fraction, color = color) }
  }
}

@Composable
private fun Bar(
  height: Float,
  color: Color,
) {
  Box(
    Modifier
      .width(4.dp)
      .height(height.dp)
      .clip(CircleShape)
      .background(color),
  )
}

private fun Context.hasMicrophonePermission() =
  ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@WearScreenPreviews
@Composable
fun DictatePreview() =
  WearPreview {
    DictateContent(personName = "Kael", text = "Li va agradar el llibre que li vaig deixar.", onFinish = {})
  }

@WearScreenPreviews
@Composable
fun DictateListeningPreview() =
  WearPreview {
    DictateContent(personName = "Kael", text = "", level = 0.6f, onFinish = {})
  }

@WearScreenPreviews
@Composable
fun DictatePermissionPreview() = WearPreview { DictatePermissionContent(onAllow = {}) }

@WearScreenPreviews
@Composable
fun DictateErrorPreview() = WearPreview { DictateErrorContent(reason = SpeechError.NoSpeech, onRetry = {}) }

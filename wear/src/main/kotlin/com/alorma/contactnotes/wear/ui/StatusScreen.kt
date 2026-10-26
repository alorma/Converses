package com.alorma.contactnotes.wear.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialogContent
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.components.CookieShape
import com.alorma.contactnotes.wear.theme.ButtonLabelStyle
import com.alorma.contactnotes.wear.theme.PersonNameStyle
import com.alorma.contactnotes.wear.theme.RowSubtitleStyle
import com.alorma.contactnotes.wear.theme.SavedTitleStyle
import com.alorma.contactnotes.wear.theme.WearColors
import com.alorma.contactnotes.wear.theme.WhoTitleStyle
import com.alorma.contactnotes.wear.theme.color

/** Mentre s'arrenca i es llegeix la còpia local: un indicador al centre, perquè la primera pantalla no quedi en blanc. */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
  val description = stringResource(R.string.loading)
  ScreenScaffold(modifier = modifier) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(
        modifier = Modifier.size(32.dp).semantics { contentDescription = description },
        strokeWidth = 4.dp,
      )
    }
  }
}

@WearScreenPreviews
@Composable
fun LoadingPreview() = WearPreview { LoadingContent() }

/** Pantalla centrada: icona sobre una Galeta, títol, text i, opcionalment, un `EdgeButton`. Serveix per a Desat, Protegida i Bloqueig. */
@Composable
fun StatusContent(
  icon: ImageVector,
  iconBackground: Color,
  iconTint: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  cookieSize: Dp = 72.dp,
  iconSize: Dp = 32.dp,
  titleStyle: androidx.compose.ui.text.TextStyle = WhoTitleStyle,
  action: String? = null,
  actionTonal: Boolean = false,
  onAction: () -> Unit = {},
) {
  // Les mides del brief són per a una pantalla de 240 dp; a Small Round (192 dp) la Galeta s'encongeix perquè hi càpiga tot
  val scale = (LocalConfiguration.current.screenHeightDp / 240f).coerceIn(0.7f, 1f)
  val cookieRotation = rememberCookieRotation()
  val spec = rememberTransformationSpec()
  val iconSlot: @Composable () -> Unit = {
    Box(modifier = Modifier.size(cookieSize * scale * scale * 0.85f), contentAlignment = Alignment.Center) {
      // Només gira la Galeta; la icona es queda quieta
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = cookieRotation.value }
            .clip(CookieShape)
            .background(iconBackground),
      )
      Icon(
        icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(iconSize * scale * scale * 0.85f),
      )
    }
  }
  val titleSlot: @Composable () -> Unit = {
    Text(
      text = title,
      style =
        titleStyle.copy(
          fontSize = titleStyle.fontSize * (0.5f + 0.5f * scale),
          lineHeight = titleStyle.lineHeight * (0.5f + 0.5f * scale),
        ),
      color = WearColors.OnSurface,
    )
  }
  val textSlot: @Composable () -> Unit = {
    Text(text = subtitle, style = RowSubtitleStyle, color = WearColors.OnSurfaceVariant)
  }
  // `AlertDialogContent` natiu: centra el contingut, hi posa els marges de la pantalla rodona i fa scroll si no hi cap
  if (action != null) {
    AlertDialogContent(
      edgeButton = {
        EdgeButton(
          onClick = onAction,
          colors = if (actionTonal) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
        ) {
          Text(text = action, style = ButtonLabelStyle)
        }
      },
      title = titleSlot,
      transformationSpec = spec,
      modifier = modifier,
      icon = iconSlot,
      text = textSlot,
    )
  } else {
    AlertDialogContent(
      title = titleSlot,
      transformationSpec = spec,
      modifier = modifier,
      icon = iconSlot,
      text = textSlot,
    )
  }
}

/** Pantalles 5 i 6: nota desada, enviada o a la cua si el mòbil no és accessible. */
@Composable
fun SavedContent(
  pendingPhone: Boolean,
  modifier: Modifier = Modifier,
) {
  if (pendingPhone) {
    StatusContent(
      icon = Icons.Rounded.Schedule,
      iconBackground = WearColors.SurfaceCardHigh,
      iconTint = WearColors.OnSurface,
      title = stringResource(R.string.saved_title),
      subtitle = stringResource(R.string.saved_pending),
      modifier = modifier,
      cookieSize = 88.dp,
      iconSize = 44.dp,
      titleStyle = SavedTitleStyle,
    )
  } else {
    StatusContent(
      icon = Icons.Rounded.Check,
      iconBackground = WearColors.Primary,
      iconTint = WearColors.OnPrimary,
      title = stringResource(R.string.saved_title),
      subtitle = stringResource(R.string.saved_sent),
      modifier = modifier,
      cookieSize = 104.dp,
      iconSize = 52.dp,
      titleStyle = SavedTitleStyle,
    )
  }
}

/** Pantalla 9: cal un bloqueig de pantalla al rellotge; substitueix qualsevol altra pantalla. */
@Composable
fun LockRequiredContent(
  onSetUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  StatusContent(
    icon = Icons.Rounded.Lock,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(R.string.lock_required_title),
    subtitle = stringResource(R.string.lock_required_hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
    action = stringResource(R.string.lock_required_action),
    onAction = onSetUp,
  )
}

@WearScreenPreviews
@Composable
fun SavedPreview() = WearPreview { SavedContent(pendingPhone = false) }

@WearScreenPreviews
@Composable
fun SavedPendingPreview() = WearPreview { SavedContent(pendingPhone = true) }

@WearScreenPreviews
@Composable
fun LockRequiredPreview() = WearPreview { LockRequiredContent(onSetUp = {}) }

private const val CookieRotationMillis = 24_000

/** `false` si l'usuari ha desactivat les animacions del sistema («reduir animacions»). */
@Composable
fun rememberAnimationsOn(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
  }
}

/** Gir lent i continu de la Galeta (una volta cada 24 s). Sense gir si l'usuari ha desactivat les animacions del sistema. */
@Composable
private fun rememberCookieRotation(): State<Float> {
  if (!rememberAnimationsOn()) return remember { mutableStateOf(0f) }
  val transition = rememberInfiniteTransition(label = "cookieRotation")
  return transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(CookieRotationMillis, easing = LinearEasing)),
    label = "cookieRotation",
  )
}

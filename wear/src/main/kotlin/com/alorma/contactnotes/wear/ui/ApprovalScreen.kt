package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PhonelinkErase
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.theme.PersonNameStyle
import com.alorma.contactnotes.wear.theme.WearColors
import com.alorma.contactnotes.wear.theme.WhoTitleStyle

/**
 * Aprovació d'una persona protegida: pantalles 7 («Confirma-ho al mòbil», amb anell de compte enrere), 10 («Denegat»),
 * 11 («Ha caducat») i 12 («No disponible sense el mòbil»). No hi surt mai el nom de la persona ni cap contingut.
 * [onApproved] es crida quan el mòbil ha aprovat: la pantalla passa a la cronologia.
 */
@Composable
fun ApprovalScreen(
  viewModel: ApprovalViewModel,
  onCancel: () -> Unit,
  onClose: () -> Unit,
  onApproved: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val currentOnApproved by rememberUpdatedState(onApproved)
  LaunchedEffect(state) {
    if (state == ApprovalUiState.Approved) currentOnApproved()
  }
  ApprovalContent(
    state = state,
    onCancel = {
      viewModel.cancel()
      onCancel()
    },
    onRetry = viewModel::retry,
    onClose = onClose,
  )
}

@Composable
fun ApprovalContent(
  state: ApprovalUiState,
  onCancel: () -> Unit,
  onRetry: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (state) {
    is ApprovalUiState.Waiting -> WaitingContent(state, onCancel, modifier)
    // Un instant, mentre la cronologia s'obre
    ApprovalUiState.Approved -> Box(modifier.fillMaxSize())
    ApprovalUiState.Denied ->
      StatusContent(
        icon = Icons.Rounded.Close,
        iconBackground = WearColors.SurfaceCardHigh,
        iconTint = WearColors.OnSurface,
        title = stringResource(R.string.approval_denied_title),
        subtitle = stringResource(R.string.approval_denied_hint),
        modifier = modifier,
        cookieSize = ApprovalCookie,
        compact = true,
        iconSize = ApprovalIcon,
        titleStyle = PersonNameStyle,
        action = stringResource(R.string.approval_close),
        actionTonal = true,
        onAction = onClose,
      )
    ApprovalUiState.Expired ->
      StatusContent(
        icon = Icons.Rounded.Schedule,
        iconBackground = WearColors.SurfaceCardHigh,
        iconTint = WearColors.OnSurface,
        title = stringResource(R.string.approval_expired_title),
        subtitle = stringResource(R.string.approval_expired_hint),
        modifier = modifier,
        cookieSize = ApprovalCookie,
        compact = true,
        iconSize = ApprovalIcon,
        titleStyle = PersonNameStyle,
        action = stringResource(R.string.approval_retry),
        onAction = onRetry,
      )
    ApprovalUiState.NoPhone ->
      StatusContent(
        icon = Icons.Rounded.PhonelinkErase,
        iconBackground = WearColors.SurfaceCardHigh,
        iconTint = WearColors.OnSurface,
        title = stringResource(R.string.approval_nophone_title),
        subtitle = stringResource(R.string.approval_nophone_hint),
        modifier = modifier,
        cookieSize = ApprovalCookie,
        compact = true,
        iconSize = ApprovalIcon,
        titleStyle = WhoTitleStyle,
        action = stringResource(R.string.approval_close),
        actionTonal = true,
        onAction = onClose,
      )
  }
}

/** Pantalla 7: l'anell de la vora es va buidant amb el compte enrere (el temps real el valida el mòbil). */
@Composable
private fun WaitingContent(
  state: ApprovalUiState.Waiting,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    StatusContent(
      icon = Icons.Rounded.Fingerprint,
      iconBackground = WearColors.SurfaceCardHigh,
      iconTint = WearColors.OnSurface,
      title = stringResource(R.string.approval_waiting_title),
      subtitle = stringResource(R.string.approval_waiting_hint, state.secondsLeft),
      cookieSize = ApprovalCookie,
      compact = true,
      iconSize = ApprovalIcon,
      titleStyle = WhoTitleStyle,
      action = stringResource(R.string.approval_cancel),
      actionTonal = true,
      onAction = onCancel,
    )
    val track = WearColors.SurfaceCardHigh
    val arc = WearColors.Primary
    val fraction = if (state.totalSeconds == 0) 0f else state.secondsLeft.toFloat() / state.totalSeconds
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stroke = RingWidth.toPx()
      val inset = stroke / 2
      val topLeft = Offset(inset, inset)
      val size = Size(this.size.width - stroke, this.size.height - stroke)
      drawArc(
        color = track,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(stroke),
      )
      drawArc(
        color = arc,
        startAngle = -90f,
        sweepAngle = 360f * fraction,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(stroke, cap = StrokeCap.Round),
      )
    }
  }
}

private val RingWidth = 7.dp

// Més petites que les de Desat: aquestes pantalles tenen títol llarg, subtítol i botó, i a Small Round no hi cabria tot
private val ApprovalCookie = 56.dp
private val ApprovalIcon = 26.dp

@WearScreenPreviews
@Composable
fun ApprovalWaitingPreview() =
  WearPreview { ApprovalContent(ApprovalUiState.Waiting(42, 60), onCancel = {}, onRetry = {}, onClose = {}) }

@WearScreenPreviews
@Composable
fun ApprovalDeniedPreview() =
  WearPreview { ApprovalContent(ApprovalUiState.Denied, onCancel = {}, onRetry = {}, onClose = {}) }

@WearScreenPreviews
@Composable
fun ApprovalExpiredPreview() =
  WearPreview { ApprovalContent(ApprovalUiState.Expired, onCancel = {}, onRetry = {}, onClose = {}) }

@WearScreenPreviews
@Composable
fun ApprovalNoPhonePreview() =
  WearPreview { ApprovalContent(ApprovalUiState.NoPhone, onCancel = {}, onRetry = {}, onClose = {}) }

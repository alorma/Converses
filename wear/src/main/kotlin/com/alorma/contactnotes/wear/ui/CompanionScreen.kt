package com.alorma.contactnotes.wear.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.theme.PersonNameStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Pantalla 17: el rellotge té l'app i el mòbil emparellat no. Substitueix la llista i no mostra cap dada. */
@Composable
fun CompanionMissingContent(
  onOpenOnPhone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  StatusContent(
    icon = Icons.Rounded.Smartphone,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(R.string.companion_missing_title),
    subtitle = stringResource(R.string.companion_missing_hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
    action = stringResource(R.string.companion_open_on_phone),
    onAction = onOpenOnPhone,
  )
}

/** Pantalla 18: després del toc, «Hem obert Google Play». */
@Composable
fun CompanionOpenedContent(
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  StatusContent(
    icon = Icons.Rounded.Check,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(R.string.companion_opened_title),
    subtitle = stringResource(R.string.companion_opened_hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
    action = stringResource(R.string.approval_close),
    actionTonal = true,
    onAction = onClose,
  )
}

@WearScreenPreviews
@Composable
fun CompanionMissingPreview() = WearPreview { CompanionMissingContent(onOpenOnPhone = {}) }

@WearScreenPreviews
@Composable
fun CompanionOpenedPreview() = WearPreview { CompanionOpenedContent(onClose = {}) }

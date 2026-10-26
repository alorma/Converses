package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.components.PersonAvatar
import com.alorma.contactnotes.wear.theme.ButtonLabelStyle
import com.alorma.contactnotes.wear.theme.WhoTitleStyle
import kotlinx.coroutines.delay

/** Pantalla 4: Jo / {persona} / Apunt; per defecte, la persona (índex 1). */
@Composable
fun WhoContent(
  personName: String,
  personInitials: String,
  personColor: AvatarColor,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()
  // Etiqueta i icona de cada opció, en l'ordre de selectedIndex
  val options: List<Pair<String, @Composable () -> Unit>> =
    listOf(
      stringResource(R.string.entry_who_me) to { Icon(Icons.Rounded.Person, contentDescription = null) },
      personName to {
        PersonAvatar(initials = personInitials, color = personColor, size = ButtonDefaults.IconSize)
      },
      stringResource(R.string.entry_who_note) to { Icon(Icons.Rounded.EditNote, contentDescription = null) },
    )
  ScreenScaffold(
    scrollState = state,
    modifier = modifier,
    edgeButton = {
      EdgeButton(onClick = onSave) { Text(text = stringResource(R.string.save), style = ButtonLabelStyle) }
    },
  ) { padding ->
    TransformingLazyColumn(
      state = state,
      contentPadding = padding,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      item { WearListTitle(text = stringResource(R.string.who_title), style = WhoTitleStyle) }
      items(options.size) { index ->
        val (label, icon) = options[index]
        RadioButton(
          selected = index == selectedIndex,
          onSelect = { onSelect(index) },
          modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
          icon = { icon() },
        ) {
          Text(text = label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@WearScreenPreviews
@Composable
fun WhoPreview() =
  WearPreview {
    var selected by remember { mutableIntStateOf(1) }
    WhoContent(
      personName = "Kael",
      personInitials = "K",
      personColor = AvatarColor.Sky,
      selectedIndex = selected,
      onSelect = { selected = it },
      onSave = {},
    )
  }

/** Play va rebutjar l'app per un nom llarg tallat a la vora rodona: aquesta captura ho vigila. */
@WearScreenPreviews
@Composable
fun WhoLongNamePreview() =
  WearPreview {
    WhoContent(
      personName = "Maria Antònia Fontcuberta i Puigdollers",
      personInitials = "MF",
      personColor = AvatarColor.Peach,
      selectedIndex = 1,
      onSelect = {},
      onSave = {},
    )
  }

/** Segons que es veu «Desat» abans de tornar a la cronologia. */
private const val SavedVisibleMillis = 2_000L

/** Pantalles 4, 5 i 6: tria qui ho diu, desa a la cua i confirma. [onDone] torna a la cronologia. */
@Composable
fun WhoScreen(
  viewModel: WhoViewModel,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current
  when (val current = state) {
    WhoUiState.Loading -> Unit
    WhoUiState.Unavailable -> LaunchedEffect(Unit) { onDone() }
    is WhoUiState.Choosing ->
      WhoContent(
        personName = current.name,
        personInitials = current.initials,
        personColor = current.color,
        selectedIndex = current.selectedIndex,
        onSelect = viewModel::select,
        onSave = viewModel::save,
        modifier = modifier,
      )
    // Nota a una protegida que no ha arribat: no s'ha desat enlloc. Es reutilitzen les pantalles 12 i 11 de l'aprovació
    is WhoUiState.NotSent ->
      ApprovalContent(
        state = if (current.phoneMissing) ApprovalUiState.NoPhone else ApprovalUiState.Expired,
        onCancel = onDone,
        // «Reintenta» (aprovació caducada): es torna enrere, on es pot tornar a obrir la persona
        onRetry = onDone,
        onClose = onDone,
        modifier = modifier,
      )
    is WhoUiState.Saved -> {
      LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(SavedVisibleMillis)
        onDone()
      }
      SavedContent(pendingPhone = current.pendingPhone, modifier = modifier)
    }
  }
}

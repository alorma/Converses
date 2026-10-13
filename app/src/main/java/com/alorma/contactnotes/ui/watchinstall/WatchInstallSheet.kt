package com.alorma.contactnotes.ui.watchinstall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.StatusBadge
import com.alorma.contactnotes.designsystem.theme.AppColors

/** Full inferior de les pantalles 19 i 20: proposa instal·lar l'app al rellotge i, després, diu com acabar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchInstallSheet(
  state: WatchInstallUiState,
  onInstall: () -> Unit,
  onNotNow: () -> Unit,
  onNeverAgain: () -> Unit,
  onDone: () -> Unit,
) {
  if (state == WatchInstallUiState.Hidden) return
  AppColors {
    ModalBottomSheet(
      onDismissRequest = if (state is WatchInstallUiState.Finish) onDone else onNotNow,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      WatchInstallContent(
        state = state,
        onInstall = onInstall,
        onNotNow = onNotNow,
        onNeverAgain = onNeverAgain,
        onDone = onDone,
      )
    }
  }
}

/** Contingut del full; el `ModalBottomSheet` és una finestra a part. */
@Composable
fun WatchInstallContent(
  state: WatchInstallUiState,
  onInstall: () -> Unit,
  onNotNow: () -> Unit,
  onNeverAgain: () -> Unit,
  onDone: () -> Unit,
) {
  val fallback = stringResource(R.string.install_watch_fallback_name)
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 24.dp)
        .padding(bottom = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    when (state) {
      WatchInstallUiState.Hidden -> Unit
      is WatchInstallUiState.Propose -> {
        StatusBadge(icon = Icons.Rounded.Watch, size = 96.dp)
        Spacer(Modifier.height(8.dp))
        Title(stringResource(R.string.install_watch_title))
        Body(stringResource(R.string.install_watch_body, state.watchName ?: fallback))
        Spacer(Modifier.height(8.dp))
        Button(onClick = onInstall, modifier = Modifier.fillMaxWidth().height(56.dp)) {
          Text(stringResource(R.string.install_watch_action), fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onNotNow) {
          Text(stringResource(R.string.install_watch_not_now), fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onNeverAgain) {
          Text(
            stringResource(R.string.install_watch_never),
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      is WatchInstallUiState.Finish -> {
        StatusBadge(icon = Icons.Rounded.WatchLater, size = 96.dp)
        Spacer(Modifier.height(8.dp))
        Title(stringResource(R.string.install_watch_finish_title))
        Body(stringResource(R.string.install_watch_finish_body, state.watchName ?: fallback))
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
          Text(stringResource(R.string.install_watch_done), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
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

@Composable
private fun Body(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
  )
}

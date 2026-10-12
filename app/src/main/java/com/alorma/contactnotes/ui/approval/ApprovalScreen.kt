package com.alorma.contactnotes.ui.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.StatusBadge

/**
 * Pantalles 14 i 16 del disseny del rellotge: la que s'obre en tocar la notificació («Confirma» / «Denega») i la de
 * «Fet». Genèriques: no hi surt mai el nom de la persona. El `BiometricPrompt` (pantalla 15) el pinta el sistema.
 */
@Composable
fun ApprovalContent(
  state: ApprovalUiState,
  onConfirm: () -> Unit,
  onDeny: () -> Unit,
  onClose: () -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      if (state == ApprovalUiState.Confirm) {
        IconButton(
          onClick = onClose,
          modifier =
            Modifier
              .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
              .padding(8.dp),
        ) {
          Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
        }
      }
    },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
      when (state) {
        ApprovalUiState.Done -> {
          Message(
            title = stringResource(R.string.approval_done_title),
            body = stringResource(R.string.approval_done_body),
            icon = Icons.Rounded.Check,
            done = true,
            modifier = Modifier.weight(1f),
          )
          Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.close))
          }
        }

        else -> {
          Message(
            title = stringResource(R.string.approval_title),
            body = stringResource(R.string.approval_hint),
            icon = Icons.Rounded.Fingerprint,
            done = false,
            modifier = Modifier.weight(1f),
          )
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp)) {
              Text(stringResource(R.string.approval_confirm))
            }
            OutlinedButton(onClick = onDeny, modifier = Modifier.fillMaxWidth().height(52.dp)) {
              Text(stringResource(R.string.approval_deny))
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun Message(
  title: String,
  body: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  done: Boolean,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    StatusBadge(
      icon = icon,
      containerColor = if (done) scheme.primary else scheme.surfaceContainerHigh,
      contentColor = if (done) scheme.onPrimary else scheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = body,
      style = MaterialTheme.typography.bodyLarge,
      color = scheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

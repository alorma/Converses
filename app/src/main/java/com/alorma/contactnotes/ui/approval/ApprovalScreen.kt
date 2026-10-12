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
 * Pantalla 14 del disseny del rellotge. Només es veu si l'usuari cancel·la el `BiometricPrompt` (que es llança
 * directament en obrir l'aprovació): «Confirma» el reobre i «Denega» denega. Genèrica: no hi surt mai el nom de la
 * persona. Després d'aprovar, l'activitat es tanca sola: no hi ha pantalla de «Fet» (decidit amb Bernat).
 */
@Composable
fun ApprovalContent(
  onConfirm: () -> Unit,
  onDeny: () -> Unit,
  onClose: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  Scaffold(
    containerColor = scheme.surface,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      IconButton(
        onClick = onClose,
        modifier =
          Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(8.dp),
      ) {
        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
      }
    },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        StatusBadge(icon = Icons.Rounded.Fingerprint)
        Spacer(Modifier.height(24.dp))
        Text(
          text = stringResource(R.string.approval_title),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = stringResource(R.string.approval_hint),
          style = MaterialTheme.typography.bodyLarge,
          color = scheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp)) {
          Text(stringResource(R.string.approval_confirm))
        }
        OutlinedButton(onClick = onDeny, modifier = Modifier.fillMaxWidth().height(52.dp)) {
          Text(stringResource(R.string.approval_deny))
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R

/**
 * Primer dispositiu, amb la sessió iniciada i abans de veure les paraules (13e): demana l'empremta i, si va bé, crida
 * [onUnlocked]. Cancel·lar-la crida [onCancel], que tanca la sessió. Pendent (fase 3, tasca B): ara no demana res.
 */
@Composable
fun SyncActivatingContent(
  padding: PaddingValues,
  onUnlocked: () -> Unit,
  onCancel: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(padding),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = onUnlocked) { Text(stringResource(R.string.sync_continue)) }
    TextButton(onClick = onCancel) { Text(stringResource(R.string.sync_cancel)) }
  }
}

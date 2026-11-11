package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R

/**
 * Les paraules de seguretat (13f). Amb [activation], porta el peu de «Les he guardat» i «Continua», i tancar-la abans
 * tanca la sessió. [onClose] tanca la pantalla en tots els casos. Pendent (fase 3, tasca B): ara no ensenya res.
 */
@Composable
fun SecurityWordsScreen(
  @Suppress("UNUSED_PARAMETER") activation: Boolean,
  onClose: () -> Unit,
) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
  }
}

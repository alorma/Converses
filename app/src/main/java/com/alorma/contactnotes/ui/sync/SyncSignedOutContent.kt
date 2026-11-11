package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R

/** Sense sessió (13d). Pendent (fase 3, tasca B): ara només hi ha el botó. */
@Composable
fun SyncSignedOutContent(
  padding: PaddingValues,
  onSignIn: () -> Unit,
) {
  Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
    Button(onClick = onSignIn) { Text(stringResource(R.string.sync_google)) }
  }
}

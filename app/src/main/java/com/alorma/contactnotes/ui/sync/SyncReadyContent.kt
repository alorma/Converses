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
import com.alorma.contactnotes.data.sync.SyncAccount

/**
 * Amb sessió i clau (13o, reduïda). «Paraules de seguretat» demana l'empremta i, si va bé, crida [onWordsUnlocked];
 * [onSignOut] es crida després de la confirmació. Pendent (fase 3, tasca B): ara només hi ha els dos botons.
 */
@Composable
fun SyncReadyContent(
  @Suppress("UNUSED_PARAMETER") account: SyncAccount,
  @Suppress("UNUSED_PARAMETER") wordsReviewedAt: Long?,
  padding: PaddingValues,
  onWordsUnlocked: () -> Unit,
  onSignOut: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(padding),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = onWordsUnlocked) { Text(stringResource(R.string.sync_words_title)) }
    TextButton(onClick = onSignOut) { Text(stringResource(R.string.sync_sign_out)) }
  }
}

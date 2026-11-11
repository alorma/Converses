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
 * El compte ja té dades i aquest dispositiu no en té la clau (13i). [onChangeAccount] tanca la sessió per entrar amb
 * un altre compte. Pendent (fase 3, tasca C): ara només hi ha els dos botons.
 */
@Composable
fun SyncNeedsKeyContent(
  @Suppress("UNUSED_PARAMETER") account: SyncAccount,
  padding: PaddingValues,
  onEnterWords: () -> Unit,
  onChangeAccount: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(padding),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = onEnterWords) { Text(stringResource(R.string.sync_key_words)) }
    TextButton(onClick = onChangeAccount) { Text(stringResource(R.string.sync_key_change_account)) }
  }
}

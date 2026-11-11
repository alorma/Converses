package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.designsystem.components.readableWidth

/**
 * El compte ja té dades i aquest dispositiu no en té la clau (13i). [onChangeAccount] tanca la sessió per entrar amb
 * un altre compte. Encara no hi ha la targeta del QR: l'única opció ocupa tot l'ample.
 */
@Composable
fun SyncNeedsKeyContent(
  account: SyncAccount,
  padding: PaddingValues,
  onEnterWords: () -> Unit,
  onChangeAccount: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .readableWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(R.string.sync_key_title), style = MaterialTheme.typography.headlineSmall)
      Text(
        stringResource(R.string.sync_key_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Card(
      onClick = onEnterWords,
      modifier = Modifier.fillMaxWidth().height(164.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
          Icon(
            Icons.Rounded.Password,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp).size(24.dp),
          )
        }
        Column {
          Text(stringResource(R.string.sync_key_words), style = MaterialTheme.typography.titleMedium)
          Text(
            stringResource(R.string.sync_key_words_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            (account.name ?: account.email)
              .orEmpty()
              .firstOrNull()
              ?.uppercase()
              .orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }
      Column(Modifier.weight(1f)) {
        Text(
          account.name ?: stringResource(R.string.sync_account),
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        account.email?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      TextButton(onClick = onChangeAccount) { Text(stringResource(R.string.sync_key_change_account)) }
    }
  }
}

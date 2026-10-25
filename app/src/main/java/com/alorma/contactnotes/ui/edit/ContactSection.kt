package com.alorma.contactnotes.ui.edit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.theme.DialogTitleStyle

/** Fila de la secció «Contacte» de l'editor (`docs/contacts/disseny.md`, 5a, 5c i 5d); amb `Hidden` no es dibuixa res. */
@Composable
internal fun ContactSectionRow(
  section: ContactSection,
  onLink: () -> Unit,
  onUnlink: () -> Unit,
  onAllow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (section) {
    ContactSection.Hidden -> Unit
    ContactSection.NotLinked, ContactSection.Taken ->
      Surface(
        onClick = onLink,
        shape = MaterialTheme.shapes.large,
        color = GroupedListDefaults.containerColor,
        modifier = modifier.fillMaxWidth(),
      ) {
        ContactRowLayout(icon = Icons.Rounded.PersonAdd) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              stringResource(R.string.contact_link),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            val taken = section == ContactSection.Taken
            Text(
              stringResource(if (taken) R.string.contact_taken else R.string.contact_link_summary),
              style = MaterialTheme.typography.bodyMedium,
              color = if (taken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    is ContactSection.Linked ->
      Surface(
        shape = MaterialTheme.shapes.large,
        color = GroupedListDefaults.containerColor,
        modifier = modifier.fillMaxWidth(),
      ) {
        ContactRowLayout(icon = Icons.Rounded.Contacts) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              stringResource(R.string.contact_linked_to),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              section.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          IconButton(onClick = onUnlink, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.LinkOff, contentDescription = stringResource(R.string.contact_unlink))
          }
        }
      }
    ContactSection.NoPermission ->
      Surface(
        shape = MaterialTheme.shapes.large,
        color = GroupedListDefaults.containerColor,
        modifier = modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            ContactIcon(Icons.Rounded.Lock)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                stringResource(R.string.contacts_no_permission),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                stringResource(R.string.contacts_no_permission_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          FilledTonalButton(
            onClick = onAllow,
            modifier = Modifier.align(Alignment.End).height(40.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          ) {
            Text(stringResource(R.string.contacts_allow), fontWeight = FontWeight.Bold)
          }
        }
      }
  }
}

@Composable
private fun ContactRowLayout(
  icon: ImageVector,
  content: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(start = 16.dp, end = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    ContactIcon(icon)
    content()
  }
}

/** Icona en un cercle de 44 dp, com a les files de Contacte de l'editor i del detall. */
@Composable
internal fun ContactIcon(icon: ImageVector) {
  Box(
    modifier =
      Modifier
        .size(44.dp)
        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
  }
}

/** Ajustos de l'app, l'únic lloc on es pot donar un permís que el sistema ja no torna a preguntar. */
internal fun Context.openAppDetailsSettings() {
  startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

/** Explicació abans de demanar el permís de Contactes (5b). «Ara no» només la tanca. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactsExplainSheet(
  onContinue: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    ContactsExplainSheetContent(onContinue = onContinue, onNotNow = onDismiss)
  }
}

/** Contingut del full 5b; el `ModalBottomSheet` és una finestra a part. */
@Composable
internal fun ContactsExplainSheetContent(
  onContinue: () -> Unit,
  onNotNow: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(stringResource(R.string.contacts_explain_title), style = DialogTitleStyle)
    Text(
      stringResource(R.string.contacts_explain_body),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ExplainPoint(
      Icons.Rounded.ManageAccounts,
      R.string.contacts_explain_account_title,
      R.string.contacts_explain_account_body,
    )
    ExplainPoint(Icons.Rounded.Inventory2, R.string.contacts_explain_copy_title, R.string.contacts_explain_copy_body)
    ExplainPoint(Icons.Rounded.Shield, R.string.contacts_explain_deny_title, R.string.contacts_explain_deny_body)
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
      TextButton(onClick = onNotNow) {
        Text(stringResource(R.string.contacts_explain_not_now), fontWeight = FontWeight.Bold)
      }
      Button(onClick = onContinue) {
        Text(stringResource(R.string.contacts_explain_continue), fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun ExplainPoint(
  icon: ImageVector,
  @StringRes title: Int,
  @StringRes body: Int,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    ContactIcon(icon)
    Column(modifier = Modifier.weight(1f)) {
      Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

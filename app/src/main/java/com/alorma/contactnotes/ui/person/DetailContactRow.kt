package com.alorma.contactnotes.ui.person

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.ui.edit.ContactIcon

/** Vincle amb Contactes al detall (`docs/contacts/disseny.md`, 5e i 5f); amb [DetailContact.None] no es dibuixa res. */
@Composable
internal fun DetailContactRow(
  contact: DetailContact,
  onOpen: () -> Unit,
  onUnlink: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (contact) {
    DetailContact.None -> Unit
    is DetailContact.Linked ->
      Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = GroupedListDefaults.containerColor,
        modifier = modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          ContactIcon(Icons.Rounded.Contacts)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              stringResource(R.string.contact_linked_name, contact.name),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              stringResource(R.string.contact_open),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    DetailContact.Deleted ->
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            ContactIcon(Icons.Rounded.PersonOff)
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
              Text(
                stringResource(R.string.contact_deleted_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                stringResource(R.string.contact_deleted_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          FilledTonalButton(
            onClick = onUnlink,
            modifier = Modifier.align(Alignment.End).height(40.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          ) {
            Text(stringResource(R.string.contact_unlink_button), fontWeight = FontWeight.Bold)
          }
        }
      }
  }
}

/** Obre el contacte a l'app Contactes; el lookup URI el troba encara que l'id hagi canviat. */
internal fun Context.openContact(contact: DetailContact.Linked) {
  val uri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
  try {
    startActivity(Intent(Intent.ACTION_VIEW, uri))
  } catch (_: ActivityNotFoundException) {
    // Sense app de Contactes: no hi ha res a obrir.
  }
}

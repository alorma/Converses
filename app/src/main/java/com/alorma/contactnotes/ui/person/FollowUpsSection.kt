package com.alorma.contactnotes.ui.person

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.designsystem.components.GroupedList
import com.alorma.contactnotes.designsystem.theme.SectionTitleStyle

/**
 * Secció «Pendents» del detall (P6), entre «Dades» i «Cronologia»: capçalera amb `flag`, títol i nombre, i una
 * fila per pendent amb el cercle «Marca com a fet» (24 dp en una àrea de 48 dp), «QUI · DIA · CANAL» i el text.
 * Tocar la fila obre el full d'accions de l'entrada.
 */
@Composable
internal fun FollowUpsSection(
  followUps: List<EntryEntity>,
  firstName: String,
  channelName: @Composable (EntryEntity) -> String,
  onDone: (EntryEntity) -> Unit,
  onOpen: (EntryEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(top = 24.dp)) {
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp).semantics(mergeDescendants = true) {
          heading()
        },
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Text(text = stringResource(R.string.follow_ups_title), style = SectionTitleStyle, modifier = Modifier.weight(1f))
      Text(
        text = followUps.size.toString(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    GroupedList(followUps) { entry ->
      FollowUpRow(
        entry = entry,
        firstName = firstName,
        channel = channelName(entry),
        onDone = { onDone(entry) },
        onOpen = { onOpen(entry) },
      )
    }
  }
}

@Composable
private fun FollowUpRow(
  entry: EntryEntity,
  firstName: String,
  channel: String,
  onDone: () -> Unit,
  onOpen: () -> Unit,
) {
  val who = whoLabel(entry.kind, firstName)
  val day = dayLabel(entryDate(entry.occurredAt))
  val description =
    stringResource(R.string.entry_description, who, day, formatTime(entry.occurredAt), channel, entry.text)
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 4.dp, end = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val markDone = stringResource(R.string.follow_up_mark_done)
    IconButton(onClick = onDone, modifier = Modifier.semantics { contentDescription = markDone }) {
      Box(Modifier.size(24.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
    }
    Column(
      modifier =
        Modifier
          .weight(1f)
          .padding(vertical = 14.dp)
          .clearAndSetSemantics { contentDescription = description },
    ) {
      Text(
        text = listOf(who, day, channel).joinToString(" · ") { it.uppercase() },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(text = entry.text, style = MaterialTheme.typography.bodyLarge)
    }
  }
}

/** Etiqueta «PENDENT» amb `flag` a la cronologia, per a les entrades amb el pendent obert. */
@Composable
internal fun FollowUpLabel(modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
      Text(
        text = stringResource(R.string.follow_up_label),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
      )
    }
  }
}

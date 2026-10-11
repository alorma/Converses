package com.alorma.contactnotes.ui.person

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.TimelineMarker
import com.alorma.contactnotes.designsystem.components.TimelineMarkerSize
import com.alorma.contactnotes.designsystem.components.TimelineMarkerStyle

private const val MaxFirstNameLength = 12

/** Primer mot del nom, tallat amb «…» perquè no passi de 12 caràcters. */
fun firstName(fullName: String): String {
  val word = fullName.trim().substringBefore(' ')
  return if (word.length > MaxFirstNameLength) word.take(MaxFirstNameLength - 1) + "…" else word
}

/** Inicial per al marcador d'avatar. */
fun initialOf(fullName: String): String = fullName.trim().take(1).uppercase()

/** Qui parla en una entrada: «Jo», el primer nom de la persona o «Apunt». */
fun whoLabel(
  kind: EntryKind,
  firstName: String,
  me: String,
  note: String,
): String =
  when (kind) {
    EntryKind.SAID -> me
    EntryKind.HEARD -> firstName
    EntryKind.HAPPENED -> note
  }

@Composable
fun whoLabel(
  kind: EntryKind,
  firstName: String,
): String = whoLabel(kind, firstName, stringResource(R.string.entry_who_me), stringResource(R.string.entry_who_note))

fun whoLabel(
  context: Context,
  kind: EntryKind,
  firstName: String,
): String =
  whoLabel(kind, firstName, context.getString(R.string.entry_who_me), context.getString(R.string.entry_who_note))

/** Nom del tipus als filtres i als comptadors: «Jo», el primer nom o «Apunts». */
@Composable
fun whoFilterLabel(
  kind: EntryKind,
  firstName: String,
): String = whoLabel(kind, firstName, stringResource(R.string.entry_who_me), stringResource(R.string.entry_who_notes))

/** Icona del tipus; `HEARD` no en té perquè fa servir l'avatar de la persona. */
fun EntryKind.whoIcon(selected: Boolean = false): ImageVector? =
  when (this) {
    EntryKind.SAID -> if (selected) Icons.Rounded.Person else Icons.Rounded.PersonOutline
    EntryKind.HEARD -> null
    EntryKind.HAPPENED -> Icons.Rounded.EditNote
  }

/** Avatar de la persona amb l'inicial, per al selector, els filtres i el marcador. */
@Composable
fun PersonInitialAvatar(
  person: PersonEntity,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Avatar(
    initials = initialOf(person.name),
    shape = person.shape,
    color = person.color,
    modifier = modifier,
    size = size,
  )
}

/** Marcador de la cronologia: cercle per a «Jo», avatar per a la persona i quadrat per a l'apunt. */
@Composable
fun EntryMarker(
  kind: EntryKind,
  person: PersonEntity,
) {
  when (kind) {
    EntryKind.SAID -> TimelineMarker(icon = Icons.Rounded.Person, style = TimelineMarkerStyle.Filled)
    EntryKind.HEARD -> TimelineMarker { PersonInitialAvatar(person, TimelineMarkerSize) }
    EntryKind.HAPPENED -> TimelineMarker(icon = Icons.Rounded.EditNote, style = TimelineMarkerStyle.Outlined)
  }
}

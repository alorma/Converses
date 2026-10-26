package com.alorma.contactnotes.wear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.wear.theme.EntryTextStyle
import com.alorma.contactnotes.wear.theme.OverlineStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Qui apareix al marcador d'una entrada: jo, la persona o un apunt. */
sealed interface EntryAuthor {
  data object Me : EntryAuthor

  data class Person(
    val initials: String,
    val color: AvatarColor,
    val shape: AvatarShape = AvatarShape.Standard,
  ) : EntryAuthor

  data object Note : EntryAuthor
}

private val MarkerSize = 16.dp

@Composable
private fun EntryMarker(author: EntryAuthor) {
  when (author) {
    EntryAuthor.Me ->
      Box(
        modifier = Modifier.size(MarkerSize).clip(CircleShape).background(WearColors.Primary),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Rounded.Person,
          contentDescription = null,
          tint = WearColors.OnPrimary,
          modifier = Modifier.size(12.dp),
        )
      }
    is EntryAuthor.Person ->
      PersonAvatar(
        initials = author.initials,
        color = author.color,
        shape = author.shape,
        size = MarkerSize,
      )
    EntryAuthor.Note ->
      Box(
        modifier = Modifier.size(MarkerSize).border(1.dp, WearColors.Outline, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Rounded.EditNote,
          contentDescription = null,
          tint = WearColors.OnSurfaceVariant,
          modifier = Modifier.size(12.dp),
        )
      }
  }
}

/** Entrada de la cronologia (només lectura): una `TitleCard` nativa amb el marcador i «qui · hora · canal» al títol. */
@Composable
fun TimelineEntry(
  author: EntryAuthor,
  meta: String,
  text: String,
  modifier: Modifier = Modifier,
  transformation: SurfaceTransformation? = null,
) {
  TitleCard(
    modifier = modifier.fillMaxWidth(),
    transformation = transformation,
    title = {
      EntryMarker(author)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = meta.uppercase(),
        style = OverlineStyle,
        color = WearColors.OnSurfaceVariant,
        letterSpacing = 0.3.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
  ) {
    Text(text = text, style = EntryTextStyle)
  }
}

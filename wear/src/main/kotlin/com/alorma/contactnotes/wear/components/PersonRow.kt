package com.alorma.contactnotes.wear.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.wear.R

/**
 * Fila de la llista de persones: el `Button` natiu de Wear, amb l'avatar Galeta a la icona, el nom i l'hora de l'última
 * entrada. Una persona protegida només mostra nom i cadenat: cap hora ni entrada.
 */
@Composable
fun PersonRow(
  name: String,
  initials: String,
  color: AvatarColor,
  avatarShape: AvatarShape,
  lastEntry: String?,
  isProtected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  transformation: SurfaceTransformation? = null,
) {
  FilledTonalButton(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    transformation = transformation,
    // Les inicials repetirien el nom a TalkBack
    icon = {
      PersonAvatar(
        initials = initials,
        color = color,
        shape = avatarShape,
        size = ButtonDefaults.LargeIconSize,
        modifier = Modifier.clearAndSetSemantics {},
      )
    },
    secondaryLabel =
      when {
        isProtected -> {
          {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = stringResource(R.string.person_protected),
              modifier = Modifier.size(16.dp),
            )
          }
        }
        lastEntry != null -> {
          { Text(text = lastEntry, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        else -> null
      },
  ) {
    Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

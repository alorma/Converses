package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ChipLabelStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight(800))
private val BadgeNumberStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight(800))

/**
 * Xip de filtre de Material 3 de 52 dp i forma de pastilla. Marcat: tinta amb icona de 22 dp. Sense marcar: fons
 * neutre i icona dins d'un quadrat de 36 dp (radi 12) del color de fons.
 */
@Composable
fun ExpressiveFilterChip(
  selected: Boolean,
  onClick: () -> Unit,
  label: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label, style = ChipLabelStyle) },
    modifier = modifier.height(52.dp),
    leadingIcon = {
      if (selected) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
      } else {
        Box(
          modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(scheme.background),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
      }
    },
    shape = CircleShape,
    colors =
      FilterChipDefaults.filterChipColors(
        containerColor = scheme.surfaceContainer,
        labelColor = scheme.onSurface,
        iconColor = scheme.onSurface,
        selectedContainerColor = scheme.primary,
        selectedLabelColor = scheme.onPrimary,
        selectedLeadingIconColor = scheme.onPrimary,
      ),
    border = null,
  )
}

/**
 * Capçalera d'un grup del llistat: insígnia amb el nombre de persones i títol display. El nombre es llegeix amb
 * [countDescription] (p. ex. «3 persones») i tot el conjunt és un encapçalament.
 */
@Composable
fun GroupHeader(
  title: String,
  count: Int,
  countDescription: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.semantics(mergeDescendants = true) { heading() },
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // La insígnia de Material (`Badge`) és de 16 dp; aquesta és una `Surface` de 34 dp (decisió de disseny).
    Surface(
      modifier =
        Modifier.size(34.dp).clearAndSetSemantics { contentDescription = countDescription },
      shape = RoundedCornerShape(11.dp),
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(count.toString(), style = BadgeNumberStyle)
      }
    }
    WideTitle(
      text = title,
      color = MaterialTheme.colorScheme.onBackground,
      maxSize = 34.sp,
      minSize = 34.sp,
      maxLines = 1,
    )
  }
}

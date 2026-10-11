package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Fila d'acció amb vora discontínua i radi 24, per afegir un element al final d'una llista. */
@Composable
fun DashedActionRow(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val outline = MaterialTheme.colorScheme.outlineVariant
  val radius = 24.dp
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(radius))
        .clickable(onClick = onClick)
        .drawBehind {
          val stroke = 2.dp.toPx()
          drawRoundRect(
            color = outline,
            topLeft =
              androidx.compose.ui.geometry
                .Offset(stroke / 2, stroke / 2),
            size =
              androidx.compose.ui.geometry
                .Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius.toPx()),
            style =
              Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
              ),
          )
        }.padding(horizontal = 16.dp, vertical = 18.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

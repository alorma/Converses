package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Vora discontínua dins del límit de la vista, amb la forma d'un rectangle arrodonit (o un cercle si `radius` és la meitat). */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.5.dp, dash: Dp = 6.dp, gap: Dp = 5.dp): Modifier =
  drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
      color = color,
      topLeft = Offset(stroke / 2, stroke / 2),
      size = Size(size.width - stroke, size.height - stroke),
      cornerRadius = CornerRadius(radius.toPx()),
      style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
    )
  }

/** Fila discontínua amb títol i pista per afegir el primer element d'una secció. */
@Composable
fun DashedHintRow(title: String, hint: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .clip(RoundedCornerShape(24.dp))
      .clickable(role = Role.Button, onClick = onClick)
      .dashedBorder(MaterialTheme.colorScheme.outline, radius = 24.dp)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Rounded.Add, contentDescription = null)
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Text(hint, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

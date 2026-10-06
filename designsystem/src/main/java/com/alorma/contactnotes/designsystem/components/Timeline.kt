package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class TimelineMarkerStyle { Filled, Tonal, Outlined }

private val MarkerSize = 40.dp
private val LineWidth = 2.dp
private val RowGap = 8.dp

/** Marcador d'una entrada: cercle ple, cercle tonal o quadrat amb vora. */
@Composable
fun TimelineMarker(icon: ImageVector, style: TimelineMarkerStyle, modifier: Modifier = Modifier) {
  val scheme = MaterialTheme.colorScheme
  val (shape, container, content, border) = when (style) {
    TimelineMarkerStyle.Filled -> MarkerColors(CircleShape, scheme.primary, scheme.onPrimary, null)
    TimelineMarkerStyle.Tonal -> MarkerColors(CircleShape, scheme.primaryContainer, scheme.onPrimaryContainer, null)
    TimelineMarkerStyle.Outlined ->
      MarkerColors(RoundedCornerShape(12.dp), scheme.surface, scheme.onSurface, BorderStroke(LineWidth, scheme.outline))
  }
  Surface(
    modifier = modifier.size(MarkerSize),
    shape = shape,
    color = container,
    contentColor = content,
    border = border,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
  }
}

private data class MarkerColors(
  val shape: androidx.compose.ui.graphics.Shape,
  val container: androidx.compose.ui.graphics.Color,
  val content: androidx.compose.ui.graphics.Color,
  val border: BorderStroke?,
)

/**
 * Fila de la cronologia amb la línia vertical contínua a l'esquerra. Sense `marker` és una capçalera de dia.
 * La línia comença al marcador a la primera fila i hi acaba a l'última.
 */
@Composable
fun TimelineRow(
  isFirst: Boolean,
  isLast: Boolean,
  modifier: Modifier = Modifier,
  marker: (@Composable () -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val lineColor = MaterialTheme.colorScheme.outlineVariant
  Row(modifier = modifier.height(IntrinsicSize.Min)) {
    Box(
      modifier = Modifier
        .width(MarkerSize)
        .fillMaxHeight()
        .drawBehind {
          val x = size.width / 2
          val markerCenter = MarkerSize.toPx() / 2
          val start = if (isFirst) (if (marker != null) markerCenter else size.height) else 0f
          val end = if (isLast) markerCenter.coerceAtMost(size.height) else size.height
          if (end > start) {
            drawLine(lineColor, Offset(x, start), Offset(x, end), strokeWidth = LineWidth.toPx())
          }
        },
      contentAlignment = Alignment.TopCenter,
    ) {
      marker?.invoke()
    }
    Box(modifier = Modifier.padding(start = 12.dp, bottom = RowGap)) {
      content()
    }
  }
}

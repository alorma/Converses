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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class TimelineMarkerStyle { Filled, Tonal, Outlined }

/** Mida del marcador i de la columna de la línia. */
val TimelineMarkerSize = 32.dp
private val MarkerSize = TimelineMarkerSize
private val LineWidth = 2.dp
private val RowGap = 8.dp

/** Marcador d'una entrada: cercle ple, cercle tonal o quadrat amb vora. */
@Composable
fun TimelineMarker(
  icon: ImageVector,
  style: TimelineMarkerStyle,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val (shape, container, content, border) =
    when (style) {
      TimelineMarkerStyle.Filled -> MarkerColors(CircleShape, scheme.primary, scheme.onPrimary, null)
      TimelineMarkerStyle.Tonal -> MarkerColors(CircleShape, scheme.primaryContainer, scheme.onPrimaryContainer, null)
      TimelineMarkerStyle.Outlined ->
        MarkerColors(
          RoundedCornerShape(10.dp),
          scheme.surface,
          scheme.onSurface,
          BorderStroke(LineWidth, scheme.outline),
        )
    }
  Surface(
    modifier = modifier.size(MarkerSize),
    shape = shape,
    color = container,
    contentColor = content,
    border = border,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
  }
}

/** Marcador fantasma: cercle amb vora discontínua i un «+», per a la primera entrada que encara no existeix. */
@Composable
fun GhostTimelineMarker(
  icon: ImageVector,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .size(
          MarkerSize,
        ).dashedBorder(MaterialTheme.colorScheme.outline, radius = MarkerSize / 2, width = LineWidth),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
  }
}

/** Marcador amb contingut propi (per exemple, l'avatar de la persona), sense fons. */
@Composable
fun TimelineMarker(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier.size(MarkerSize), contentAlignment = Alignment.Center) { content() }
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
  lineProgress: () -> Float = { 1f },
  dashedLine: Boolean = false,
  content: @Composable () -> Unit,
) {
  val lineColor = MaterialTheme.colorScheme.outlineVariant
  Row(modifier = modifier.height(IntrinsicSize.Min)) {
    Box(
      modifier =
        Modifier
          .width(MarkerSize)
          .fillMaxHeight()
          .drawBehind {
            val x = size.width / 2
            val markerCenter = MarkerSize.toPx() / 2
            val start = if (isFirst) (if (marker != null) markerCenter else size.height) else 0f
            val end = if (isLast) markerCenter.coerceAtMost(size.height) else size.height
            if (end > start) {
              val drawnEnd = start + (end - start) * lineProgress()
              drawLine(
                lineColor,
                Offset(x, start),
                Offset(x, drawnEnd),
                strokeWidth = LineWidth.toPx(),
                pathEffect =
                  if (dashedLine) {
                    PathEffect.dashPathEffect(
                      floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                    )
                  } else {
                    null
                  },
              )
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

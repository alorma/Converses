@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * Botó amb la forma d'un polígon arrodonit, basat en el `FilledIconButton` de Material 3 (rol de botó, ripple,
 * àrea tàctil mínima i animació de la forma amb l'`MotionScheme` del tema). En prémer-lo la forma fa un morph cap
 * a un cercle; amb [selected] és un cercle de manera permanent. Les animacions del sistema desactivades les
 * respecta el mateix Material.
 */
@Composable
fun ShapeButton(
  onClick: () -> Unit,
  shape: RoundedPolygon,
  size: Dp,
  containerColor: Color,
  contentColor: Color,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  content: @Composable () -> Unit,
) {
  val circle = MaterialShapes.Circle.toShape()
  val resting = shape.toShape()
  val shapes =
    remember(circle, resting, selected) {
      IconButtonShapes(shape = if (selected) circle else resting, pressedShape = circle)
    }
  FilledIconButton(
    onClick = onClick,
    shapes = shapes,
    modifier =
      modifier
        .size(size)
        .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    colors =
      IconButtonDefaults.filledIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
      ),
    content = content,
  )
}

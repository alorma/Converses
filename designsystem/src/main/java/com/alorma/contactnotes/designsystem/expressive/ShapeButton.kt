@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * Botó amb la forma d'un polígon arrodonit, basat en el `FilledIconButton` de Material 3 (ripple i colors).
 * En prémer-lo, o mentre [selected] és cert, la forma fa un morph cap a un cercle amb el `fastSpatialSpec`
 * del tema ([MorphShape]; Material només anima formes de cantonades arrodonides). Amb les animacions del
 * sistema desactivades ([rememberReduceMotion]) salta directament a la forma final.
 * La icona es dibuixa amb la mida dels tokens d'`IconButtonDefaults` segons [size]: petita (24 dp) per sota de 56 dp,
 * gran (32 dp) fins a 80 dp i extra gran (40 dp) per sobre. Hi ha un sol focus de teclat (el de l'àrea exterior).
 * Si [selected] no és nul, el botó és un commutador i ho exposa a l'accessibilitat.
 * L'àrea tàctil és com a mínim de 48 dp encara que [size] sigui més petit.
 */
@Composable
fun ShapeButton(
  onClick: () -> Unit,
  shape: RoundedPolygon,
  size: Dp,
  containerColor: Color,
  contentColor: Color,
  contentDescription: String?,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  selected: Boolean? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val reduceMotion = rememberReduceMotion()
  val progress by animateFloatAsState(
    targetValue = if (pressed || selected == true) 1f else 0f,
    animationSpec = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
    label = "shapeButtonMorph",
  )
  val iconSize =
    when {
      size < 56.dp -> IconButtonDefaults.smallIconSize
      size <= 80.dp -> IconButtonDefaults.largeIconSize
      else -> IconButtonDefaults.extraLargeIconSize
    }
  val morph = remember(shape) { Morph(shape.normalized(), ExpressiveShapes.Circle.normalized()) }
  val morphShape = remember(morph, progress) { MorphShape(morph, progress.coerceIn(0f, 1f)) }

  // L'àrea tàctil exterior (>= 48 dp) té el clic i la semàntica; el botó de Material és només la part visible.
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .semantics {
          if (contentDescription != null) this.contentDescription = contentDescription
          if (selected != null) this.selected = selected
        }.clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
  ) {
    FilledIconButton(
      onClick = onClick,
      enabled = enabled,
      modifier = Modifier.size(size).focusProperties { canFocus = false }.clearAndSetSemantics {},
      shape = morphShape,
      colors =
        IconButtonDefaults.filledIconButtonColors(
          containerColor = containerColor,
          contentColor = contentColor,
        ),
      interactionSource = interactionSource,
      content = { Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize)) },
    )
  }
}

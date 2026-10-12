package com.alorma.contactnotes.wear.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Efecte de scroll de Wear: els elements s'encongeixen i s'esvaeixen en acostar-se a la vora de la pantalla rodona
 * (al brief, la fila de Marta és més petita per això).
 */
fun Modifier.scrollTransformed(
  scope: TransformingLazyColumnItemScope,
  spec: TransformationSpec,
): Modifier =
  transformedHeight(scope, spec).graphicsLayer {
    with(scope) { with(spec) { applyContainerTransformation(scrollProgress) } }
  }

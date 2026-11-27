package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

/**
 * Forma que dibuixa el [morph] a l'instant [progress] (0 = forma inicial, 1 = forma final).
 * El camí del `Morph` és en coordenades 0..1 (polígons normalitzats) i s'escala a la mida rebuda.
 */
class MorphShape(
  private val morph: Morph,
  private val progress: Float,
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val path = morph.toPath(progress).asComposePath()
    val matrix = Matrix()
    matrix.scale(size.width, size.height)
    path.transform(matrix)
    return Outline.Generic(path)
  }
}

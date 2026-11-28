@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon

/** Formes de l'app expressiva. Exposa les de `MaterialShapes` perquè `:app` no hagi de fer l'opt-in experimental. */
object ExpressiveShapes {
  /** Botons d'acció: nota ràpida, «Desa», afegir persona, «Nova tasca». */
  val Cookie9Sided: RoundedPolygon get() = MaterialShapes.Cookie9Sided

  /** Avatar gran del detall i botó seleccionat del selector Qui. */
  val Clover4Leaf: RoundedPolygon get() = MaterialShapes.Clover4Leaf

  /** Decoració de la targeta destacada. */
  val Flower: RoundedPolygon get() = MaterialShapes.Flower

  /** Forma final del morph en prémer un [ShapeButton]. */
  val Circle: RoundedPolygon get() = MaterialShapes.Circle
}

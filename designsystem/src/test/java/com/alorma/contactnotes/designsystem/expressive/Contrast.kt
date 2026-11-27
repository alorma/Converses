package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Relació de contrast WCAG entre dos colors (de 1 a 21). */
internal fun contrast(
  a: Color,
  b: Color,
): Double {
  val l1 = a.luminance().toDouble()
  val l2 = b.luminance().toDouble()
  return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
}

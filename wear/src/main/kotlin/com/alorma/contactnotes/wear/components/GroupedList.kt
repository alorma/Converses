package com.alorma.contactnotes.wear.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object GroupedDefaults {
  val OuterCorner = 24.dp
  val InnerCorner = 6.dp
  val ItemSpacing = 2.dp
}

/** Cantonades segons la posició: 24 dp als extrems del grup, 6 dp entre files. */
fun groupedItemShape(
  index: Int,
  count: Int,
): RoundedCornerShape {
  val top = if (index == 0) GroupedDefaults.OuterCorner else GroupedDefaults.InnerCorner
  val bottom = if (index == count - 1) GroupedDefaults.OuterCorner else GroupedDefaults.InnerCorner
  return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

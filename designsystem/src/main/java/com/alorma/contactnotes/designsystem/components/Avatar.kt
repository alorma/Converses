package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AvatarColor(val color: Color) {
  Lavender(Color(0xFFDCD3F5)),
  Mint(Color(0xFFC9EBD6)),
  Peach(Color(0xFFFFD6BF)),
  Sky(Color(0xFFCBE3F7)),
  Rose(Color(0xFFF7CFDD)),
  Lemon(Color(0xFFF1E9A8)),
  Sand(Color(0xFFE8DDCB)),
}

enum class AvatarShape { Cookie9Sided, Clover4Leaf, Flower, Sunny }

private val AvatarInitialsColor = Color(0xFF1D1B20)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvatarShape.asShape(): Shape = when (this) {
  AvatarShape.Cookie9Sided -> MaterialShapes.Cookie9Sided
  AvatarShape.Clover4Leaf -> MaterialShapes.Clover4Leaf
  AvatarShape.Flower -> MaterialShapes.Flower
  AvatarShape.Sunny -> MaterialShapes.Sunny
}.toShape()

/** Avatar amb forma expressiva i color pastel. Les inicials sempre en fosc, també en mode fosc. */
@Composable
fun Avatar(
  initials: String,
  shape: AvatarShape,
  color: AvatarColor,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(shape.asShape())
      .background(color.color),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initials,
      color = AvatarInitialsColor,
      fontWeight = FontWeight.Bold,
      fontSize = (size.value * 0.36f).sp,
    )
  }
}

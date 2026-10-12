package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape

/** Color pastel de l'avatar. */
val AvatarColor.color: Color get() = Color(argb)

/** Llavor de l'esquema de color de la persona. */
val AvatarColor.seed: Color get() = Color(seedArgb)

internal val AvatarInitialsColor = Color(0xFF1E1D24)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvatarShape.asShape(): Shape =
  when (this) {
    AvatarShape.Cookie9Sided -> MaterialShapes.Cookie9Sided
    AvatarShape.Clover4Leaf -> MaterialShapes.Clover4Leaf
    AvatarShape.Clover8Leaf -> MaterialShapes.Clover8Leaf
    AvatarShape.Sunny -> MaterialShapes.Sunny
    AvatarShape.Ghostish -> MaterialShapes.Ghostish
    AvatarShape.PuffyDiamond -> MaterialShapes.PuffyDiamond
  }.toShape()

/** Avatar amb forma expressiva i color pastel. Les inicials sempre en fosc, també en mode fosc. */
@Composable
fun Avatar(
  initials: String,
  shape: AvatarShape,
  color: AvatarColor,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  locked: Boolean = false,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(shape.asShape())
        .background(color.color),
    contentAlignment = Alignment.Center,
  ) {
    if (locked) {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = AvatarInitialsColor,
        modifier = Modifier.size(size * 0.42f),
      )
    } else {
      Text(
        text = initials,
        color = AvatarInitialsColor,
        fontWeight = FontWeight.Bold,
        fontSize = with(LocalDensity.current) { (size * 0.36f).toSp() },
      )
    }
  }
}

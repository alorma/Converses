package com.alorma.contactnotes.wear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.wear.theme.AvatarColor
import com.alorma.contactnotes.wear.theme.AvatarInk

/** Galeta de 9 pètals, la mateixa forma que l'avatar del mòbil (`MaterialShapes.Cookie9Sided`). */
private val Cookie9Sided: RoundedPolygon =
  RoundedPolygon.star(
    numVerticesPerRadius = 9,
    innerRadius = 0.8f,
    rounding = CornerRounding(radius = 0.5f),
  )

private class PolygonShape(
  private val polygon: RoundedPolygon,
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val bounds = polygon.calculateBounds()
    val path = polygon.toPath().asComposePath()
    val matrix = Matrix()
    // Les operacions s'apliquen als punts en ordre invers: primer es trasllada a l'origen i després s'escala
    matrix.scale(size.width / (bounds[2] - bounds[0]), size.height / (bounds[3] - bounds[1]))
    matrix.translate(-bounds[0], -bounds[1])
    path.transform(matrix)
    return Outline.Generic(path)
  }
}

val CookieShape: Shape = PolygonShape(Cookie9Sided)

/** Avatar Galeta amb color pastel. El text i la icona van sempre en tinta fosca. */
@Composable
fun PersonAvatar(
  initials: String,
  color: AvatarColor,
  modifier: Modifier = Modifier,
  size: Dp = 36.dp,
  locked: Boolean = false,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(CookieShape)
        .background(color.color),
    contentAlignment = Alignment.Center,
  ) {
    if (locked) {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = AvatarInk,
        modifier = Modifier.size(size * 0.42f),
      )
    } else {
      Text(
        text = initials,
        color = AvatarInk,
        fontWeight = FontWeight.ExtraBold,
        fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() },
      )
    }
  }
}

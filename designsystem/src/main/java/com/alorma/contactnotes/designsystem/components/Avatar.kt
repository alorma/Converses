package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.unit.sp
import java.text.Normalizer

/** Pastel fix de l'avatar i llavor de l'esquema de color de la persona. */
enum class AvatarColor(val color: Color, val seed: Color) {
  Lavender(Color(0xFFDCD3F5), Color(0xFF6B55B5)),
  Mint(Color(0xFFC9EBD6), Color(0xFF2F7A52)),
  Peach(Color(0xFFFFD6BF), Color(0xFFB5602E)),
  Sky(Color(0xFFCBE3F7), Color(0xFF2D6FA3)),
  Rose(Color(0xFFF7CFDD), Color(0xFFB04670)),
  Lemon(Color(0xFFF1E9A8), Color(0xFF8A7A12)),
  Sand(Color(0xFFE8DDCB), Color(0xFF86704C));

  companion object {
    /** Color automàtic: el mateix nom (sense majúscules, accents ni espais als extrems) dona sempre el mateix color. */
    fun forName(name: String): AvatarColor {
      val normalized = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
      // FNV-1a: estable entre execucions i reparteix millor els noms curts que `String.hashCode`
      var hash = -0x7ee3623b
      for (char in normalized) hash = (hash xor char.code) * 16777619
      return entries[Math.floorMod(hash, entries.size)]
    }
  }
}

enum class AvatarShape {
  Cookie9Sided,
  Clover4Leaf,
  Clover8Leaf,
  Sunny,
  Ghostish,
  PuffyDiamond;

  companion object {
    /** Forma de l'avatar estàndard, la mateixa per a tothom. */
    val Standard = Cookie9Sided
  }
}

internal val AvatarInitialsColor = Color(0xFF1E1D24)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvatarShape.asShape(): Shape = when (this) {
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
    modifier = modifier
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

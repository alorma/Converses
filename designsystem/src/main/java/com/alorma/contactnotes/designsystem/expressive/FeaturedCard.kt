@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val CardHeight = 268.dp
private val CardPadding = 24.dp
private val AddSize = 84.dp

/**
 * Targeta destacada del llistat: la persona de l'última conversa, amb la cita. Un `Card` de Material 3 amb els
 * colors `featured`/`onFeatured` de la persona, una `Flower` decorativa i una galeta per afegir-li una entrada.
 * L'alçada és un mínim: amb text llarg o lletra gran la targeta creix, i la cita (4 línies com a molt) no arriba
 * a la galeta. El `Card` fusiona avatar, sobretítol i cita en un sol node; la galeta és un botó a part.
 */
@Composable
fun FeaturedCard(
  overline: String,
  quote: String,
  palette: PersonPalette,
  onClick: () -> Unit,
  onAdd: () -> Unit,
  addDescription: String,
  avatar: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val flower = remember { ExpressiveShapes.Flower }
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(40.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = palette.featured,
        contentColor = palette.onFeatured,
      ),
  ) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = CardHeight)) {
      Box(
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .offset(x = 70.dp, y = (-60).dp)
            .size(250.dp)
            .clip(flower.toShape())
            .background(palette.onFeatured.copy(alpha = 0.12f)),
      )
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = CardPadding, top = CardPadding, end = CardPadding, bottom = CardPadding + AddSize + 16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          avatar()
          Spacer(Modifier.width(10.dp))
          Text(
            text = overline,
            style = ExpressiveType.overline,
            color = palette.onFeatured.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(Modifier.size(12.dp))
        Text(
          text = quote,
          style = ExpressiveType.quote(27.sp).copy(lineHeight = 1.2.em),
          color = palette.onFeatured,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
      }
      ShapeButton(
        onClick = onAdd,
        shape = ExpressiveShapes.Cookie9Sided,
        size = AddSize,
        containerColor = palette.onFeatured,
        contentColor = palette.featured,
        contentDescription = addDescription,
        icon = Icons.Rounded.Add,
        iconSize = 36.dp,
        modifier = Modifier.align(Alignment.BottomEnd).padding(CardPadding),
      )
    }
  }
}

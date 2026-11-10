package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Etiqueta «PRO» de 20 dp al costat d'una funció de Pro (disseny de Pro, 26-10-2026). */
@Composable
fun ProBadge(
  text: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  // El text de l'etiqueta no escala amb la mida de lletra del sistema (la descripció sí que es llegeix)
  val fontSize = with(LocalDensity.current) { 10.5.dp.toSp() }
  val letterSpacing = with(LocalDensity.current) { 0.6.dp.toSp() }
  val lineHeight = with(LocalDensity.current) { 13.dp.toSp() }
  Box(
    modifier =
      modifier
        .height(20.dp)
        .clip(RoundedCornerShape(7.dp))
        .background(scheme.primaryContainer)
        .padding(horizontal = 7.dp)
        .semantics { this.contentDescription = contentDescription },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = scheme.onPrimaryContainer,
      fontSize = fontSize,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = letterSpacing,
      lineHeight = lineHeight,
      maxLines = 1,
      softWrap = false,
    )
  }
}

/**
 * Píndola «PRO» sobre la cantonada d'un avatar (11a): fons `primary`, vora de 2 dp del color de la targeta. És decorativa:
 * qui la posa diu a la contentDescription de l'avatar que és una funció Pro.
 */
@Composable
fun ProPill(
  text: String,
  borderColor: Color,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val fontSize = with(LocalDensity.current) { 10.dp.toSp() }
  val letterSpacing = with(LocalDensity.current) { 0.6.dp.toSp() }
  val lineHeight = with(LocalDensity.current) { 12.dp.toSp() }
  val shape = RoundedCornerShape(10.dp)
  Box(
    modifier =
      modifier
        .height(20.dp)
        .clip(shape)
        .background(scheme.primary)
        .border(2.dp, borderColor, shape)
        .padding(horizontal = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = scheme.onPrimary,
      fontSize = fontSize,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = letterSpacing,
      lineHeight = lineHeight,
      maxLines = 1,
      softWrap = false,
    )
  }
}

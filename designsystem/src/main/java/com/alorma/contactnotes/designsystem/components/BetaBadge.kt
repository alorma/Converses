package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Píndola tonal de 22 dp amb el text de la versió (p. ex. «BETA»), al costat del títol de la llista. */
@Composable
fun BetaBadge(text: String, contentDescription: String, modifier: Modifier = Modifier) {
  val scheme = MaterialTheme.colorScheme
  Box(
    modifier = modifier
      .height(22.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(scheme.primaryContainer)
      .padding(horizontal = 8.dp)
      .semantics { this.contentDescription = contentDescription },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = scheme.onPrimaryContainer,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 0.8.sp,
      maxLines = 1,
    )
  }
}

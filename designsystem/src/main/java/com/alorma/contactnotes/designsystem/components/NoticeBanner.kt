package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avís no bloquejant amb colors invertits (`primary` / `on-primary`) i un sol botó per descartar-lo.
 * No es tanca sol: s'ha de veure. Es llegeix com a estat (`liveRegion = Polite`).
 */
@Composable
fun NoticeBanner(
  title: String,
  body: String,
  actionLabel: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(24.dp)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation = 8.dp, shape = shape, ambientColor = ShadowColor, spotColor = ShadowColor)
      .clip(shape)
      .background(scheme.primary)
      .padding(start = 16.dp, top = 16.dp, end = 12.dp, bottom = 12.dp)
      .semantics { liveRegion = LiveRegionMode.Polite },
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(scheme.onPrimary),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = scheme.primary)
      }
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, color = scheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
          text = body,
          color = scheme.onPrimary,
          fontSize = 14.sp,
          lineHeight = 19.6.sp,
          modifier = Modifier.alpha(0.82f),
        )
      }
    }
    Box(
      modifier = Modifier
        .align(Alignment.End)
        .height(40.dp)
        .clip(CircleShape)
        .background(scheme.onPrimary)
        .clickable(role = Role.Button, onClick = onAction)
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = actionLabel, color = scheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
  }
}

private val ShadowColor = Color.Black.copy(alpha = 0.18f)

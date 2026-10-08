package com.alorma.contactnotes.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.theme.DialogTitleStyle
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Estat buit centrat: icona, títol i, opcionalment, un botó per sortir-ne. */
@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(72.dp)) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
      }
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      FilledTonalButton(onClick = onAction) { Text(actionLabel, fontWeight = FontWeight.Bold) }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/**
 * Estat buit de benvinguda (encara no hi ha cap element): forma gran amb icona, títol, descripció i un botó primari.
 * Pensat per ocupar tota l'àrea de contingut i quedar centrat.
 */
@Composable
fun WelcomeEmptyState(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  actionIcon: ImageVector? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    // La forma gira molt lentament (una volta cada 60 s); la icona no gira
    val rotation by rememberInfiniteTransition(label = "cookie").animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(durationMillis = 60_000, easing = LinearEasing)),
      label = "cookieRotation",
    )
    // Easter egg: tocar la forma la canvia per un LoadingIndicator que passa de pressa per diverses formes
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(false) }
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(120.dp)
        .pointerInput(Unit) {
          detectTapGestures {
            if (playing) return@detectTapGestures
            scope.launch {
              playing = true
              delay(2.5.seconds)
              playing = false
            }
          }
        },
    ) {
      if (playing) {
        // L'indicador dibuixa la forma al ~80 % de la seva mida
        LoadingIndicator(
          modifier = Modifier.requiredSize(150.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          polygons = remember {
            listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Flower, MaterialShapes.Sunny)
          },
        )
      } else {
        Surface(
          shape = AvatarShape.Cookie9Sided.asShape(),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxSize().rotate(rotation),
        ) {}
      }
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(44.dp))
    }
    Spacer(Modifier.height(32.dp))
    Text(
      text = title,
      style = DialogTitleStyle,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(28.dp))
      Button(onClick = onAction, modifier = Modifier.heightIn(min = 56.dp)) {
        if (actionIcon != null) {
          Icon(actionIcon, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.size(8.dp))
        }
        Text(actionLabel, fontWeight = FontWeight.Bold)
      }
    }
  }
}

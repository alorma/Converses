@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * FAB mitjà (80 dp) de Material 3 amb forma de galeta de 9 pètals que fa un morph cap a un cercle en prémer-lo
 * ([rememberMorphToCircle]). Color préssec fix i icona en el préssec clar (`tertiaryFixed`, contrast AA).
 */
@Composable
fun ShapeFab(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  MediumFloatingActionButton(
    onClick = onClick,
    modifier = modifier,
    shape = rememberMorphToCircle(ExpressiveShapes.Cookie9Sided, active = pressed),
    containerColor = MaterialTheme.colorScheme.onTertiaryFixedVariant,
    contentColor = MaterialTheme.colorScheme.tertiaryFixed,
    interactionSource = interactionSource,
  ) {
    Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(32.dp))
  }
}

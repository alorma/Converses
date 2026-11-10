package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rail de navegació de les pantalles grans (a partir de 600 dp): fa la feina de [ConversesFloatingToolbar], amb el botó
 * de nota ràpida a dalt en lloc del FAB.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesNavigationRail(
  fabIcon: ImageVector,
  fabContentDescription: String,
  onFabClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  WideNavigationRail(
    modifier = modifier,
    colors = WideNavigationRailDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    header = {
      // La capçalera del rail s'alinea a l'inici: la caixa de l'amplada del rail centra el botó sobre les destinacions
      Box(modifier = Modifier.width(RailWidth), contentAlignment = Alignment.Center) {
        FloatingActionButton(
          onClick = onFabClick,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          // Dins del rail no flota sobre res: sense ombra
          elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        ) {
          Icon(imageVector = fabIcon, contentDescription = fabContentDescription)
        }
      }
    },
    content = content,
  )
}

/** Amplada del rail plegat de Material 3 Expressive. */
private val RailWidth = 96.dp

/** Destinació del rail: [selectedIcon] (plena) quan és l'activa, amb l'indicador del color primari. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RailDestination(
  icon: ImageVector,
  selectedIcon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  WideNavigationRailItem(
    selected = selected,
    onClick = onClick,
    icon = { Icon(imageVector = if (selected) selectedIcon else icon, contentDescription = null) },
    label = { Text(label) },
    modifier = modifier,
    // El rail no es desplega mai: sempre la icona amb l'etiqueta a sota
    railExpanded = false,
    colors =
      WideNavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedIndicatorColor = MaterialTheme.colorScheme.primary,
        selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
      ),
  )
}

/** Amplada màxima d'una columna de lectura (Ajustos, Pro) en una pantalla gran. */
val ReadableMaxWidth: Dp = 640.dp

/**
 * Columna centrada de [maxWidth] com a màxim. En una pantalla més estreta (el mòbil) no canvia res: ocupa tota
 * l'amplada. Va després de `verticalScroll`, perquè es pugui fer scroll des dels marges.
 */
fun Modifier.readableWidth(maxWidth: Dp = ReadableMaxWidth): Modifier =
  fillMaxWidth().wrapContentWidth().widthIn(max = maxWidth).fillMaxWidth()

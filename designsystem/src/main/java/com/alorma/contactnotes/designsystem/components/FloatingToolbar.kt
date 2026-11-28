package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.expressive.ShapeFab

/** A partir d'aquesta mida de font l'etiqueta de la destinació marcada no hi cap i es queda només la icona. */
private const val LabelHiddenFontScale = 1.5f

/**
 * Barra d'eines flotant: barra de 68 dp en tinta (`primary`) amb el [ShapeFab] a la dreta. El FAB es mesura primer
 * (80 dp sencer) i la barra ocupa l'espai que queda, amb 8 dp de separació mínima.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFloatingToolbar(
  onFabClick: () -> Unit,
  fabIcon: ImageVector,
  fabContentDescription: String,
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    HorizontalFloatingToolbar(
      expanded = true,
      modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp).height(68.dp),
      colors =
        FloatingToolbarDefaults.standardFloatingToolbarColors(
          toolbarContainerColor = MaterialTheme.colorScheme.primary,
          toolbarContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      shape = RoundedCornerShape(34.dp),
      content = content,
    )
    ShapeFab(icon = fabIcon, contentDescription = fabContentDescription, onClick = onFabClick)
  }
}

/**
 * Destí de [ExpressiveFloatingToolbar] sobre el `ToggleButton` de Material 3: marcat és una píndola de 48 dp amb
 * icona i text sobre `secondaryContainer`; sense marcar, només la icona (amb [label] com a descripció). Amb una
 * font molt gran la marcada també es queda només amb la icona.
 *
 * Si [isAction] és cert la destinació no és una pestanya (p. ex. Ajustos): és un botó, sense estat marcat.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveToolbarDestination(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isAction: Boolean = false,
) {
  val scheme = MaterialTheme.colorScheme
  val showLabel = selected && LocalDensity.current.fontScale < LabelHiddenFontScale
  val sized = modifier.height(PillHeight)
  val padding =
    if (showLabel) PaddingValues(start = 12.dp, end = 16.dp) else PaddingValues(horizontal = 12.dp)
  val body: @Composable () -> Unit = {
    if (showLabel) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
      Spacer(Modifier.width(6.dp))
      Text(
        text = label,
        fontSize = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    } else {
      Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
    }
  }
  if (isAction) {
    Button(
      onClick = onClick,
      modifier = sized,
      shape = CircleShape,
      colors =
        ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = scheme.onPrimary),
      contentPadding = padding,
      content = { body() },
    )
  } else {
    ToggleButton(
      checked = selected,
      onCheckedChange = { onClick() },
      modifier = sized,
      shapes = ToggleButtonShapes(CircleShape, CircleShape, CircleShape),
      colors =
        ToggleButtonDefaults.colors(
          containerColor = Color.Transparent,
          contentColor = scheme.onPrimary,
          checkedContainerColor = scheme.secondaryContainer,
          checkedContentColor = scheme.onSecondaryContainer,
        ),
      contentPadding = padding,
      content = { body() },
    )
  }
}

private val PillHeight = 48.dp

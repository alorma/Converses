package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.designsystem.theme.SectionTitleStyle

/** Comportament de scroll de la barra: el títol gran es plega al fer scroll i la barra es queda fixa a dalt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberTopBarScrollBehavior(): TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

/** Barra superior amb el títol a la segona línia (mitjana, o gran si `large`); el títol es plega en fer scroll i la barra es queda fixa. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesTopBar(
  title: String,
  scrollBehavior: TopAppBarScrollBehavior,
  modifier: Modifier = Modifier,
  large: Boolean = false,
  navigationIcon: @Composable () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  /** Va just després del títol (p. ex. l'etiqueta PRO). */
  titleTrailing: (@Composable () -> Unit)? = null,
) {
  // La mitjana pinta el títol desplegat amb headlineSmall i la gran amb headlineMedium; plegat sempre titleLarge
  MaterialExpressiveTheme(
    colorScheme = MaterialTheme.colorScheme,
    motionScheme = MotionScheme.expressive(),
    shapes = MaterialTheme.shapes,
    typography =
      MaterialTheme.typography.copy(
        headlineSmall = ScreenTitleStyle,
        headlineMedium = ScreenTitleStyle,
        titleLarge = SectionTitleStyle,
      ),
  ) {
    val colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
      )
    val titleContent: @Composable () -> Unit = {
      Row(
        modifier = if (titleTrailing != null) Modifier.padding(end = 16.dp) else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = title,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
          // Amb alguna cosa al costat (l'etiqueta PRO), el títol s'encongeix abans de tallar-se
          autoSize = if (titleTrailing != null) TextAutoSize.StepBased(minFontSize = 20.sp) else null,
        )
        titleTrailing?.invoke()
      }
    }
    if (large) {
      LargeTopAppBar(
        title = titleContent,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
      )
    } else {
      MediumTopAppBar(
        title = titleContent,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
      )
    }
  }
}

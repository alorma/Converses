package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeSource
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import com.alorma.contactnotes.wear.theme.ConversesWearTheme
import com.alorma.contactnotes.wear.theme.WearColors

/** Font de l'hora de `TimeText`; les previews en fixen una perquè les captures no depenguin del rellotge. */
val LocalWearTimeSource = staticCompositionLocalOf<TimeSource?> { null }

@Composable
fun WearTimeText() {
  val fixed = LocalWearTimeSource.current
  if (fixed != null) {
    TimeText(timeSource = fixed)
  } else {
    TimeText(timeSource = TimeTextDefaults.rememberTimeSource(TimeTextDefaults.timeFormat()))
  }
}

/** Les previews i les captures es fan sempre en català (com el brief) i a Large i Small Round. */
@Preview(
  name = "Large Round",
  device = Devices.WEAR_OS_LARGE_ROUND,
  locale = "ca",
  showBackground = true,
  backgroundColor = 0xFF000000,
)
@Preview(
  name = "Small Round",
  device = Devices.WEAR_OS_SMALL_ROUND,
  locale = "ca",
  showBackground = true,
  backgroundColor = 0xFF000000,
)
annotation class WearScreenPreviews

private val PreviewTimeSource =
  object : TimeSource {
    @Composable
    override fun currentTime(): String = "12:33"
  }

@Composable
internal fun WearPreview(content: @Composable () -> Unit) {
  ConversesWearTheme {
    CompositionLocalProvider(LocalWearTimeSource provides PreviewTimeSource) {
      AppScaffold(timeText = { WearTimeText() }) { content() }
    }
  }
}

/**
 * Pantalla de Wear amb una llista que fa scroll: el `ScreenScaffold` natiu hi posa els marges (un percentatge de la
 * pantalla, no `dp` fixos) i, si n'hi ha, l'`EdgeButton`. Així res no queda tallat per la vora rodona, ni amb textos
 * llargs: Play ho exigeix («Watch shapes»).
 */
@Composable
fun WearListScreen(
  modifier: Modifier = Modifier,
  edgeButton: (@Composable BoxScope.() -> Unit)? = null,
  verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
  content: TransformingLazyColumnScope.() -> Unit,
) {
  val state = rememberTransformingLazyColumnState()
  val list: @Composable BoxScope.(PaddingValues) -> Unit = { padding ->
    TransformingLazyColumn(
      state = state,
      contentPadding = padding,
      verticalArrangement = verticalArrangement,
      content = content,
    )
  }
  if (edgeButton != null) {
    ScreenScaffold(scrollState = state, edgeButton = edgeButton, modifier = modifier, content = list)
  } else {
    ScreenScaffold(scrollState = state, modifier = modifier, content = list)
  }
}

/**
 * Títol del primer element d'una llista: `ListHeader` hi afegeix marge lateral i el baixa fins on el cercle ja és prou
 * ample. El text va centrat, en dues línies com a màxim i amb el·lipsi: un nom llarg no surt mai de la pantalla.
 */
@Composable
fun TransformingLazyColumnItemScope.WearListTitle(
  text: String,
  style: TextStyle,
  modifier: Modifier = Modifier,
  leading: (@Composable () -> Unit)? = null,
) {
  ListHeader(
    modifier =
      modifier
        .fillMaxWidth()
        .minimumVerticalContentPadding(top = ListHeaderDefaults.minimumTopListContentPadding, bottom = 0.dp),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      leading?.invoke()
      Text(
        text = text,
        style = style,
        color = WearColors.OnSurface,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
      )
    }
  }
}

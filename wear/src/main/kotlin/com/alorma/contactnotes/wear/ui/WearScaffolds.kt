package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
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

/** Alçada que ocupa l'`EdgeButton` a baix: el contingut que hi ha a sobre ha de deixar-hi aquest marge. */
val PinnedEdgeButtonSpace = 56.dp

/**
 * Marges d'una llista: els laterals i el de dalt són els que dona `ScreenScaffold` (un percentatge de la pantalla, no
 * `dp` fixos), perquè la vora rodona no talli res a cap mida de rellotge. Play ho exigeix («Watch shapes»).
 */
@Composable
fun wearListPadding(
  scaffoldPadding: PaddingValues,
  bottom: Dp,
): PaddingValues {
  val direction = LocalLayoutDirection.current
  return PaddingValues(
    start = scaffoldPadding.calculateStartPadding(direction),
    end = scaffoldPadding.calculateEndPadding(direction),
    top = scaffoldPadding.calculateTopPadding(),
    bottom = bottom,
  )
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
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      leading?.invoke()
      Text(
        text = text,
        style = style,
        color = WearColors.OnSurface,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * `EdgeButton` sempre visible (P2). El de `ScreenScaffold` s'amaga fins que la llista arriba al final, i el brief el vol fix:
 * és l'acció principal. Porta un degradat a darrere perquè el contingut s'esvaeixi sota el botó.
 */
@Composable
fun BoxScope.PinnedEdgeButton(
  onClick: () -> Unit,
  tonal: Boolean = false,
  content: @Composable RowScope.() -> Unit,
) {
  Box(
    modifier =
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(64.dp)
        .background(Brush.verticalGradient(0f to Color.Transparent, 0.75f to WearColors.Background)),
  )
  EdgeButton(
    onClick = onClick,
    modifier = Modifier.align(Alignment.BottomCenter),
    colors = if (tonal) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
    content = content,
  )
}

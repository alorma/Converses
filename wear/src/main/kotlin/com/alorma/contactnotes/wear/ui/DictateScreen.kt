package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.theme.ButtonLabelStyle
import com.alorma.contactnotes.wear.theme.DictationTextStyle
import com.alorma.contactnotes.wear.theme.OverlineStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Alçades i colors de les ones decoratives del brief (8 barres; les del mig, més clares i altes). */
private val VoiceBars =
  listOf(
    12 to WearColors.Outline,
    20 to WearColors.OnSurfaceVariant,
    28 to WearColors.Primary,
    32 to WearColors.Primary,
    24 to WearColors.Primary,
    30 to WearColors.Primary,
    18 to WearColors.OnSurfaceVariant,
    12 to WearColors.Outline,
  )

/** Pantalla 3: la transcripció en directe, les ones decoratives i «Acaba». */
@Composable
fun DictateContent(
  personName: String,
  partialText: String,
  onFinish: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ScreenScaffold(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(start = 30.dp, end = 30.dp, top = 38.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.note_for, personName).uppercase(),
        style = OverlineStyle,
        color = WearColors.OnSurfaceVariant,
      )
      Text(text = partialText, style = DictationTextStyle, color = WearColors.OnSurface, textAlign = TextAlign.Center)
    }
    VoiceWaves(modifier = Modifier.align(Alignment.Center).padding(top = 56.dp))
    PinnedEdgeButton(onClick = onFinish) {
      Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
      Text(text = stringResource(R.string.dictate_finish), style = ButtonLabelStyle)
    }
  }
}

@Composable
private fun VoiceWaves(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.height(32.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    VoiceBars.forEach { (height, color) -> Bar(height = height, color = color) }
  }
}

@Composable
private fun Bar(
  height: Int,
  color: Color,
) {
  Box(
    Modifier
      .width(4.dp)
      .height(height.dp)
      .clip(CircleShape)
      .background(color),
  )
}

@WearScreenPreviews
@Composable
fun DictatePreview() =
  WearPreview {
    DictateContent(personName = "Kael", partialText = "Li va agradar el llibre que li vaig deixar.", onFinish = {})
  }

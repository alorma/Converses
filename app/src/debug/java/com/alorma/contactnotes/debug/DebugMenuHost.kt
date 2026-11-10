package com.alorma.contactnotes.debug

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import kotlin.math.roundToInt

private val ButtonSize = 56.dp

/** Wraps the app with a draggable floating button that opens the debug sheet. Debug builds only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenuHost(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val store = remember { DebugButtonStore(context) }
  var showSheet by remember { mutableStateOf(false) }
  // Top-start position in px; null until the button is first dragged or placed at its default spot
  var position by remember { mutableStateOf(store.loadPosition()) }

  BoxWithConstraints {
    val density = LocalDensity.current
    val buttonSize = with(density) { ButtonSize.toPx() }
    val maxX = (with(density) { maxWidth.toPx() } - buttonSize).coerceAtLeast(0f)
    val maxY = (with(density) { maxHeight.toPx() } - buttonSize).coerceAtLeast(0f)
    val defaultPosition =
      with(density) { Offset(maxX - 8.dp.toPx(), maxY - 96.dp.toPx() - WindowInsets.navigationBars.getBottom(density)) }
    // Clamped on every read, so a saved position stays reachable after a rotation or on another screen size
    val current = (position ?: defaultPosition).let { Offset(it.x.coerceIn(0f, maxX), it.y.coerceIn(0f, maxY)) }

    content()
    FloatingActionButton(
      onClick = { showSheet = true },
      modifier =
        Modifier
          .offset { IntOffset(current.x.roundToInt(), current.y.roundToInt()) }
          .pointerInput(maxX, maxY) {
            var dragged = current
            detectDragGestures(
              onDragStart = {
                dragged =
                  position?.let { Offset(it.x.coerceIn(0f, maxX), it.y.coerceIn(0f, maxY)) } ?: current
              },
              onDragEnd = { store.savePosition(dragged) },
            ) { change, drag ->
              change.consume()
              dragged = Offset((dragged.x + drag.x).coerceIn(0f, maxX), (dragged.y + drag.y).coerceIn(0f, maxY))
              position = dragged
            }
          },
    ) {
      Icon(Icons.Filled.BugReport, contentDescription = stringResource(R.string.debug_menu_open))
    }
    if (showSheet) {
      // The sheet is a dialog window: it inherits FLAG_SECURE from the activity window by default
      ModalBottomSheet(
        onDismissRequest = { showSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
          Text(
            stringResource(R.string.debug_menu_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
          )
          ConversesDebugMenu(onClose = { showSheet = false })
        }
      }
    }
  }
}

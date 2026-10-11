package com.alorma.contactnotes.debug

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import kotlin.math.roundToInt

/** Wraps the app with a draggable floating button that opens the debug sheet. Debug builds only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenuHost(content: @Composable () -> Unit) {
  var showSheet by remember { mutableStateOf(false) }
  var dragOffset by remember { mutableStateOf(IntOffset.Zero) }

  Box {
    content()
    SmallFloatingActionButton(
      onClick = { showSheet = true },
      modifier =
        Modifier
          .align(Alignment.BottomEnd)
          .navigationBarsPadding()
          .padding(end = 8.dp, bottom = 96.dp)
          .offset { dragOffset }
          .pointerInput(Unit) {
            detectDragGestures { change, drag ->
              change.consume()
              dragOffset += IntOffset(drag.x.roundToInt(), drag.y.roundToInt())
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
          ConversesDebugMenu()
        }
      }
    }
  }
}

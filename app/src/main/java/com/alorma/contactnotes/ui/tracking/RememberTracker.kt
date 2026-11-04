package com.alorma.contactnotes.ui.tracking

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.alorma.contactnotes.data.tracking.Action
import com.alorma.contactnotes.data.tracking.EventTracker
import org.koin.compose.koinInject

/**
 * Per a les accions que no passen pel ViewModel (obrir un full o un diàleg). Va a la pantalla amb estat; a les
 * previews i les captures no fa res, perquè no tenen Koin.
 */
@Composable
fun rememberActionTracker(): (Action) -> Unit {
  if (LocalInspectionMode.current) return {}
  val tracker: EventTracker = koinInject()
  return tracker::trackAction
}

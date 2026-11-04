package com.alorma.contactnotes.ui.tracking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalInspectionMode
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.Screen
import org.koin.compose.koinInject

/**
 * Envia [screen] cada cop que la pantalla entra a la composició (també en tornar-hi amb enrere), no a cada
 * recomposició. Va a la pantalla amb estat, no al `...Content`: les previews i les captures no tenen Koin.
 */
@Composable
fun TrackScreen(screen: Screen) {
  if (LocalInspectionMode.current) return
  val tracker: EventTracker = koinInject()
  LaunchedEffect(screen.eventName, screen.eventVersion, screen.eventProperties) {
    tracker.trackScreen(screen)
  }
}

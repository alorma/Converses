package com.alorma.contactnotes.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.data.flags.rememberFeatureEnabled
import com.alorma.contactnotes.data.sync.SyncFlag
import com.alorma.contactnotes.ui.navigation.EnterWordsRoute
import com.alorma.contactnotes.ui.navigation.SlotsRoute
import com.alorma.contactnotes.ui.navigation.TopLevelBackStack
import org.koin.compose.viewmodel.koinViewModel

/**
 * Obre les places plenes (13l) quan el repositori en té una pregunta i les tanca quan desapareix. Amb [SyncFlag]
 * apagat no fa res ni crea cap ViewModel. Si les paraules (13j) encara són a dalt de la pila s'espera que es tanquin:
 * acaben amb `pop()` i se'n durien la pantalla de places.
 */
@Composable
fun SyncPromptsEffect(navigation: TopLevelBackStack) {
  if (!rememberFeatureEnabled<SyncFlag>()) return
  val viewModel: SyncPromptsViewModel = koinViewModel()
  val slotsOpen by viewModel.slotsOpen.collectAsStateWithLifecycle()
  val top = navigation.current
  LaunchedEffect(slotsOpen, top) {
    when {
      !slotsOpen -> navigation.dismiss(SlotsRoute)
      top != SlotsRoute && top != EnterWordsRoute && SlotsRoute !in navigation.backStack ->
        navigation.navigate(
          SlotsRoute,
        )
    }
  }
}

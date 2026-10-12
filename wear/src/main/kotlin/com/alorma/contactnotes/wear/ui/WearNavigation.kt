package com.alorma.contactnotes.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.alorma.contactnotes.wear.data.AndroidSpeechSource
import com.alorma.contactnotes.wear.data.GmsMoreEntriesSource
import com.alorma.contactnotes.wear.data.GmsPhoneConnection
import com.alorma.contactnotes.wear.data.WearSnapshotStore
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object PeopleRoute : NavKey

@Serializable
data class PersonRoute(
  val id: Long,
) : NavKey

/** Només es crea per a persones no protegides; les protegides arriben a la fase 5. */
@Serializable
data class DictateRoute(
  val personId: Long,
  val name: String,
) : NavKey

@Composable
fun WearNavigation() {
  val context = LocalContext.current.applicationContext
  val store = remember { WearSnapshotStore.get(context) }
  val phone = remember { GmsPhoneConnection(context) }
  val labels = remember { AndroidWearLabels(context) }
  val moreEntries = remember { GmsMoreEntriesSource(context) }
  val speech = remember { AndroidSpeechSource(context) }
  val backStack = rememberNavBackStack(PeopleRoute)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        entry<PeopleRoute> {
          val vm = viewModel { PeopleViewModel(store.stored, phone, labels) }
          PeopleScreen(
            viewModel = vm,
            // Les protegides s'obriran a la fase 5 (desbloqueig); fins llavors el toc no fa res
            onPersonClick = { if (!it.isProtected) backStack.add(PersonRoute(it.id)) },
          )
        }
        entry<PersonRoute> { route ->
          val vm =
            viewModel<TimelineViewModel>(
              key = "person-${route.id}",
              factory =
                viewModelFactory {
                  initializer {
                    TimelineViewModel(
                      route.id,
                      store.stored,
                      labels,
                      moreEntries,
                    )
                  }
                },
            )
          TimelineScreen(
            viewModel = vm,
            onBack = { backStack.removeLastOrNull() },
            onNote = { name -> backStack.add(DictateRoute(route.id, name)) },
          )
        }
        entry<DictateRoute> { route ->
          val vm =
            viewModel<DictationViewModel>(
              key = "dictate-${route.personId}",
              factory =
                viewModelFactory {
                  initializer { DictationViewModel(speech, Locale.getDefault().toLanguageTag()) }
                },
            )
          // La fase 7 envia el text a «Qui»; de moment «Acaba» torna a la cronologia
          DictateScreen(viewModel = vm, personName = route.name, onClose = { backStack.removeLastOrNull() })
        }
      },
  )
}

package com.alorma.contactnotes.wear.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.alorma.contactnotes.wear.data.AndroidDeviceLock
import com.alorma.contactnotes.wear.data.AndroidSpeechSource
import com.alorma.contactnotes.wear.data.AppLockGate
import com.alorma.contactnotes.wear.data.GmsMoreEntriesSource
import com.alorma.contactnotes.wear.data.GmsPhoneConnection
import com.alorma.contactnotes.wear.data.WearNotes
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

/** «Qui» i «Desat»: [text] és la transcripció del dictat, que només viu en memòria i després a la cua. */
@Serializable
data class WhoRoute(
  val personId: Long,
  val text: String,
) : NavKey

/** Obre els ajustos de seguretat del rellotge; si el sistema no els ofereix, els ajustos generals; si no, res. */
private fun openSecuritySettings(context: Context) {
  for (action in listOf(Settings.ACTION_SECURITY_SETTINGS, Settings.ACTION_SETTINGS)) {
    try {
      context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      return
    } catch (_: ActivityNotFoundException) {
      // Es prova amb el següent
    }
  }
}

@Composable
fun WearNavigation() {
  val context = LocalContext.current.applicationContext
  val store = remember { WearSnapshotStore.get(context) }
  val phone = remember { GmsPhoneConnection(context) }
  val labels = remember { AndroidWearLabels(context) }
  val moreEntries = remember { GmsMoreEntriesSource(context) }
  val speech = remember { AndroidSpeechSource(context) }
  val outbox = remember { WearNotes.get(context) }
  val backStack = rememberNavBackStack(PeopleRoute)
  val gate =
    remember { AppLockGate(store.stored, AndroidDeviceLock(context)) { store.enforceLock() } }
  // El bloqueig es revisa en tornar a primer pla: l'usuari pot haver-lo tret mentre l'app era fora
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { gate.recheck() }
  val lockRequired by gate.lockRequired.collectAsStateWithLifecycle(initialValue = null)

  // Mentre no se sap, no es mostra res; amb «Cal un bloqueig» no es compon cap altra pantalla
  when (lockRequired) {
    null -> return
    true -> {
      LockRequiredContent(onSetUp = { openSecuritySettings(context) })
      return
    }
    false -> Unit
  }

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
          DictateScreen(
            viewModel = vm,
            personName = route.name,
            onDone = { text -> backStack.add(WhoRoute(route.personId, text)) },
          )
        }
        entry<WhoRoute> { route ->
          val vm =
            viewModel<WhoViewModel>(
              key = "who-${route.personId}",
              factory =
                viewModelFactory {
                  initializer { WhoViewModel(route.personId, route.text, store.stored, outbox, phone) }
                },
            )
          // Després de «Desat» es torna a la cronologia: es treuen «Qui» i el dictat
          WhoScreen(
            viewModel = vm,
            onDone = {
              backStack.removeLastOrNull()
              backStack.removeLastOrNull()
            },
          )
        }
      },
  )
}

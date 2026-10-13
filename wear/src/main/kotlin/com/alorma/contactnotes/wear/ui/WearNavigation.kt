package com.alorma.contactnotes.wear.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
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
import com.alorma.contactnotes.wear.data.ApprovalResults
import com.alorma.contactnotes.wear.data.DataStoreDiscardedNotes
import com.alorma.contactnotes.wear.data.GmsApprovalClient
import com.alorma.contactnotes.wear.data.GmsMoreEntriesSource
import com.alorma.contactnotes.wear.data.GmsNoteSender
import com.alorma.contactnotes.wear.data.GmsPhoneConnection
import com.alorma.contactnotes.wear.data.GmsPhoneInstall
import com.alorma.contactnotes.wear.data.ProtectedSessions
import com.alorma.contactnotes.wear.data.WearNotes
import com.alorma.contactnotes.wear.data.WearSnapshotStore
import com.alorma.contactnotes.wear.data.withProtectedSessions
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object PeopleRoute : NavKey

@Serializable
data class PersonRoute(
  val id: Long,
) : NavKey

/** Espera l'aprovació al mòbil d'una persona protegida (pantalles 7, 10, 11 i 12). */
@Serializable
data class ApprovalRoute(
  val personId: Long,
) : NavKey

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
  val approvals = remember { GmsApprovalClient(context) }
  val directSender = remember { GmsNoteSender(context) }
  val sessions = remember { ProtectedSessions.shared }
  // Una protegida aprovada es veu com una persona normal mentre la seva sessió és oberta, i només en memòria
  val openSnapshots = remember { store.stored.withProtectedSessions(sessions.all) }
  val companion = viewModel { CompanionViewModel(GmsPhoneInstall(context)) }
  val companionState by companion.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current
  // Sense l'app al mòbil emparellat no hi ha res a mostrar: ni la llista, ni la còpia local, ni el bloqueig
  when (val state = companionState) {
    CompanionUiState.Loading -> return
    is CompanionUiState.Missing -> {
      CompanionMissingContent(onOpenOnPhone = { companion.openOnPhone(state.node) })
      return
    }
    CompanionUiState.Opened -> {
      CompanionOpenedContent(onClose = { activity?.finish() })
      return
    }
    CompanionUiState.Ready -> Unit
  }
  val backStack = rememberNavBackStack(PeopleRoute)
  // En sortir d'una persona se n'oblida la sessió (si era protegida, les entrades marxen de la memòria)
  val pop: () -> Unit = {
    val top = backStack.removeLastOrNull()
    if (top is PersonRoute) sessions.close(top.id)
  }
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
    onBack = pop,
    sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        entry<PeopleRoute> {
          val vm = viewModel { PeopleViewModel(store.stored, phone, labels, DataStoreDiscardedNotes.get(context)) }
          PeopleScreen(
            viewModel = vm,
            // Una protegida s'obre només amb l'aprovació al mòbil; una de normal, directament
            onPersonClick = { backStack.add(if (it.isProtected) ApprovalRoute(it.id) else PersonRoute(it.id)) },
          )
        }
        entry<ApprovalRoute> { route ->
          val vm =
            viewModel<ApprovalViewModel>(
              key = "approval-${route.personId}",
              factory =
                viewModelFactory {
                  initializer { ApprovalViewModel(route.personId, approvals, ApprovalResults.results, sessions) }
                },
            )
          ApprovalScreen(
            viewModel = vm,
            onCancel = pop,
            onClose = pop,
            // Aprovada: la pantalla d'espera se substitueix per la cronologia, i en tornar enrere es va a la llista
            onApproved = {
              backStack.removeLastOrNull()
              backStack.add(PersonRoute(route.personId))
            },
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
                      openSnapshots,
                      labels,
                      moreEntries,
                    )
                  }
                },
            )
          TimelineScreen(
            viewModel = vm,
            onBack = pop,
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
                  initializer {
                    WhoViewModel(
                      route.personId,
                      route.text,
                      openSnapshots,
                      outbox,
                      phone,
                      sessions = sessions,
                      directSender = directSender,
                    )
                  }
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

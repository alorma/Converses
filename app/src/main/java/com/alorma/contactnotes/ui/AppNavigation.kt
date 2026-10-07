package com.alorma.contactnotes.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.alorma.contactnotes.ui.edit.EditPersonScreen
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteScreen
import com.alorma.contactnotes.ui.onboarding.OnboardingScreen
import com.alorma.contactnotes.ui.people.PeopleListRoute
import com.alorma.contactnotes.ui.person.PersonDetailRoute
import com.alorma.contactnotes.ui.person.PersonLockedScreen
import com.alorma.contactnotes.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object OnboardingRoute : NavKey

@Serializable
data object PeopleRoute : NavKey

@Serializable
data object ArchivedRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class EditPersonRoute(val personId: Long = 0L, val returnSelection: Boolean = false) : NavKey

@Serializable
data class QuickNoteRoute(val personId: Long = 0L) : NavKey

@Serializable
data class PersonRoute(val id: Long) : NavKey

@Serializable
data class PersonLockedRoute(val id: Long) : NavKey

@Composable
fun AppNavigation(startOnboarding: Boolean = false) {
  val backStack = rememberNavBackStack(if (startOnboarding) OnboardingRoute else PeopleRoute)
  val selectionBridge: PersonSelectionBridge = koinInject()

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<OnboardingRoute> {
        OnboardingScreen(
          onFinished = {
            backStack.clear()
            backStack.add(PeopleRoute)
          },
        )
      }
      entry<PeopleRoute> {
        PeopleListRoute(
          archived = false,
          onToggleArchivedList = { backStack.add(ArchivedRoute) },
          onPersonClick = { person ->
            backStack.add(if (person.isProtected) PersonLockedRoute(person.id) else PersonRoute(person.id))
          },
          onQuickNote = { backStack.add(QuickNoteRoute()) },
          onNewPerson = { backStack.add(EditPersonRoute()) },
          onOpenSettings = { backStack.add(SettingsRoute) },
        )
      }
      entry<ArchivedRoute> {
        PeopleListRoute(
          archived = true,
          onToggleArchivedList = { backStack.removeLastOrNull() },
          onPersonClick = { person ->
            backStack.add(if (person.isProtected) PersonLockedRoute(person.id) else PersonRoute(person.id))
          },
          onQuickNote = { backStack.add(QuickNoteRoute()) },
          onNewPerson = { backStack.add(EditPersonRoute()) },
          onOpenSettings = { backStack.add(SettingsRoute) },
        )
      }
      entry<QuickNoteRoute> { route ->
        QuickNoteScreen(
          personId = route.personId,
          onClose = { backStack.removeLastOrNull() },
          onNewPerson = { backStack.add(EditPersonRoute(returnSelection = true)) },
        )
      }
      entry<SettingsRoute> {
        SettingsScreen(onBack = { backStack.removeLastOrNull() })
      }
      entry<PersonLockedRoute> { route ->
        PersonLockedScreen(
          personId = route.id,
          onBack = { backStack.removeLastOrNull() },
          onUnlocked = {
            backStack.removeLastOrNull()
            backStack.add(PersonRoute(route.id))
          },
        )
      }
      entry<PersonRoute> { route ->
        PersonDetailRoute(
          personId = route.id,
          onBack = { backStack.removeLastOrNull() },
          onEdit = { backStack.add(EditPersonRoute(route.id)) },
        )
      }
      entry<EditPersonRoute> { route ->
        EditPersonScreen(
          personId = route.personId,
          onClose = { backStack.removeLastOrNull() },
          onSaved = { id ->
            backStack.removeLastOrNull()
            if (route.returnSelection) {
              selectionBridge.post(id)
            } else if (route.personId == 0L) {
              backStack.add(PersonRoute(id))
            }
          },
          onDeleted = {
            backStack.removeLastOrNull()
            backStack.removeLastOrNull()
          },
        )
      }
    },
  )
}

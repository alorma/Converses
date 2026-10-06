package com.alorma.contactnotes.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.alorma.contactnotes.ui.people.PeopleListRoute
import com.alorma.contactnotes.ui.person.PersonLockedScreen
import com.alorma.contactnotes.ui.person.PersonPlaceholderScreen
import kotlinx.serialization.Serializable

@Serializable
data object PeopleRoute : NavKey

@Serializable
data class PersonRoute(val id: Long) : NavKey

@Serializable
data class PersonLockedRoute(val id: Long) : NavKey

@Composable
fun AppNavigation() {
  val backStack = rememberNavBackStack(PeopleRoute)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<PeopleRoute> {
        PeopleListRoute(
          onPersonClick = { person ->
            backStack.add(if (person.isProtected) PersonLockedRoute(person.id) else PersonRoute(person.id))
          },
          onQuickNote = {},
        )
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
        PersonPlaceholderScreen(
          personId = route.id,
          onBack = { backStack.removeLastOrNull() },
        )
      }
    },
  )
}

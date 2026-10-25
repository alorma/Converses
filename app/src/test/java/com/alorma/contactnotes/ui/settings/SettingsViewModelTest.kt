package com.alorma.contactnotes.ui.settings

import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.testing.FakeContactsPermission
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.FakeSettingsRepository
import com.alorma.contactnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  @get:Rule val main = MainDispatcherRule()

  @Test
  fun everyChangeReachesTheRepositoryAndComesBackInTheState() =
    runTest {
      val repository = FakeSettingsRepository()
      val vm = SettingsViewModel(repository, FakeDb().repository, FakeContactsPermission())
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.settings.collect {} }

      assertFalse(vm.settings.value.dynamicColor)

      vm.onThemeMode(ThemeMode.Dark)
      vm.onDynamicColor(true)
      vm.onPersonColorTheme(false)
      vm.onHapticFeedback(false)
      vm.onAppLock(true)
      vm.onRelockAfter(300)

      val settings = vm.settings.value
      assertEquals(ThemeMode.Dark, settings.themeMode)
      assertTrue(settings.dynamicColor)
      assertFalse(settings.personColorTheme)
      assertFalse(settings.hapticFeedback)
      assertTrue(settings.appLock)
      assertEquals(300, settings.relockAfterSeconds)
    }

  @Test
  fun contactsStateFollowsPermissionExplanationAndLinkedCount() =
    runTest {
      val db = FakeDb()
      val permission = FakeContactsPermission(granted = false)
      val repository = FakeSettingsRepository()
      val vm = SettingsViewModel(repository, db.repository, permission)
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.contacts.collect {} }

      assertEquals(ContactsSettingsState.NeverOn, vm.contacts.value)

      vm.onContactsExplained()
      assertEquals(ContactsSettingsState.Denied, vm.contacts.value)

      permission.granted = true
      vm.onContactsPermissionChanged()
      assertEquals(ContactsSettingsState.On(0), vm.contacts.value)

      val ana = db.addPerson("Ana", archived = true)
      val bea = db.addPerson("Bea", isProtected = true)
      db.addPerson("Carla")
      db.people.update { list ->
        list.map {
          if (it.id == ana.id ||
            it.id == bea.id
          ) {
            it.copy(contactId = 1, contactLookupKey = "k${it.id}")
          } else {
            it
          }
        }
      }
      assertEquals(ContactsSettingsState.On(2), vm.contacts.value)

      permission.granted = false
      vm.onContactsPermissionChanged()
      assertEquals(ContactsSettingsState.Denied, vm.contacts.value)
    }
}

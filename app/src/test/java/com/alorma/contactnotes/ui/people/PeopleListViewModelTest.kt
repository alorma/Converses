package com.alorma.contactnotes.ui.people

import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleListViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()

  private fun names(vm: PeopleListViewModel) = vm.uiState.value.people.map { it.person.name }

  @Test
  fun filtersByChannelButKeepsTheUnfilteredTotal() = runTest {
    val chat = db.addPerson("Chat", createdAt = 3)
    val social = db.addPerson("Social", createdAt = 2)
    val inPerson = db.addPerson("InPerson", createdAt = 1)
    db.addChannel(chat.id, ChannelType.CHAT)
    db.addChannel(social.id, ChannelType.SOCIAL)
    db.addChannel(inPerson.id, ChannelType.IN_PERSON)
    val vm = PeopleListViewModel(archived = false, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    assertEquals(listOf("Chat", "Social", "InPerson"), names(vm))

    vm.onFilterToggled(ChannelType.CHAT)
    assertEquals(listOf("Chat"), names(vm))
    vm.onFilterToggled(ChannelType.IN_PERSON)
    assertEquals(listOf("Chat", "InPerson"), names(vm))
    assertEquals(3, vm.uiState.value.total)

    vm.onFilterToggled(ChannelType.CHAT)
    vm.onFilterToggled(ChannelType.IN_PERSON)
    assertEquals(listOf("Chat", "Social", "InPerson"), names(vm))
  }

  @Test
  fun filteringBySocialExcludesOtherChannels() = runTest {
    val onX = db.addPerson("OnSocial", createdAt = 2)
    val onInstagram = db.addPerson("OnOther", createdAt = 1)
    db.addChannel(onX.id, ChannelType.SOCIAL)
    db.addChannel(onInstagram.id, ChannelType.OTHER)
    val vm = PeopleListViewModel(archived = false, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.onFilterToggled(ChannelType.SOCIAL)
    assertEquals(listOf("OnSocial"), names(vm))
  }

  @Test
  fun protectedPeopleOnlyShowUpWithoutFilterBecauseTheirChannelsAreHidden() = runTest {
    val secret = db.addPerson("Luna", isProtected = true)
    db.addChannel(secret.id, ChannelType.CHAT)
    val vm = PeopleListViewModel(archived = false, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    assertEquals(listOf("Luna"), names(vm))
    vm.onFilterToggled(ChannelType.CHAT)
    assertEquals(emptyList<String>(), names(vm))
  }

  @Test
  fun archivedListIgnoresTheChannelFilter() = runTest {
    db.addPerson("Active")
    db.addPerson("Archived", archived = true)
    val vm = PeopleListViewModel(archived = true, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.onFilterToggled(ChannelType.PHONE)

    assertEquals(listOf("Archived"), names(vm))
    assertFalse(vm.uiState.value.isLoading)
  }
}

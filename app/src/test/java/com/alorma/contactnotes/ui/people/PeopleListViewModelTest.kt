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
  fun filtersByChannelGroupButKeepsTheUnfilteredTotal() = runTest {
    val chat = db.addPerson("Chat", createdAt = 3)
    val social = db.addPerson("Social", createdAt = 2)
    val inPerson = db.addPerson("InPerson", createdAt = 1)
    db.addChannel(chat.id, ChannelType.WHATSAPP)
    db.addChannel(social.id, ChannelType.LINKEDIN)
    db.addChannel(inPerson.id, ChannelType.IN_PERSON)
    val vm = PeopleListViewModel(archived = false, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    assertEquals(listOf("Chat", "Social", "InPerson"), names(vm))

    vm.onFilterToggled(ChannelGroup.Chat)
    assertEquals(listOf("Chat"), names(vm))
    vm.onFilterToggled(ChannelGroup.InPerson)
    assertEquals(listOf("Chat", "InPerson"), names(vm))
    assertEquals(3, vm.uiState.value.total)

    vm.onFilterToggled(ChannelGroup.Chat)
    vm.onFilterToggled(ChannelGroup.InPerson)
    assertEquals(listOf("Chat", "Social", "InPerson"), names(vm))
  }

  @Test
  fun xAndInstagramBelongToTheSocialFilter() = runTest {
    val onX = db.addPerson("OnX", createdAt = 2)
    val onInstagram = db.addPerson("OnInstagram", createdAt = 1)
    db.addChannel(onX.id, ChannelType.X)
    db.addChannel(onInstagram.id, ChannelType.INSTAGRAM)
    val vm = PeopleListViewModel(archived = false, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.onFilterToggled(ChannelGroup.Social)
    assertEquals(listOf("OnX", "OnInstagram"), names(vm))
  }

  @Test
  fun protectedPeopleOnlyShowUpWithoutFilterBecauseTheirChannelsAreHidden() = runTest {
    val secret = db.addPerson("Luna", isProtected = true)
    db.addChannel(secret.id, ChannelType.WHATSAPP)
    val vm = PeopleListViewModel(archived = false, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    assertEquals(listOf("Luna"), names(vm))
    vm.onFilterToggled(ChannelGroup.Chat)
    assertEquals(emptyList<String>(), names(vm))
  }

  @Test
  fun archivedListIgnoresTheChannelFilter() = runTest {
    db.addPerson("Active")
    db.addPerson("Archived", archived = true)
    val vm = PeopleListViewModel(archived = true, repository = db.repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.onFilterToggled(ChannelGroup.Phone)

    assertEquals(listOf("Archived"), names(vm))
    assertFalse(vm.uiState.value.isLoading)
  }
}

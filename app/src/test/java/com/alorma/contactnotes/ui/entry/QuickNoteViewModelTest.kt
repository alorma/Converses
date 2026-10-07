package com.alorma.contactnotes.ui.entry

import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickNoteViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()
  private val bridge = PersonSelectionBridge()

  private fun TestScope.viewModel(initialPersonId: Long = 0L): QuickNoteViewModel {
    val vm = QuickNoteViewModel(initialPersonId, db.repository, bridge)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    return vm
  }

  @Test
  fun startsWithoutAPersonSelected() = runTest {
    db.addPerson("Marta")

    val state = viewModel().uiState.value

    assertNull(state.selected)
    assertEquals(1, state.people.size)
  }

  @Test
  fun selectingAPersonLoadsItsChannelsAndTheChannelOfTheLastEntry() = runTest {
    val marta = db.addPerson("Marta")
    db.addChannel(marta.id, ChannelType.WHATSAPP)
    db.addChannel(marta.id, ChannelType.OTHER, "Signal")
    db.addEntry(marta.id, channel = ChannelType.OTHER, occurredAt = 9_000)
    db.addEntry(marta.id, channel = ChannelType.WHATSAPP, occurredAt = 1_000)
    val vm = viewModel()

    vm.select(marta)

    val state = vm.uiState.value
    assertEquals(marta.id, state.selected?.id)
    assertEquals(listOf(ChannelType.WHATSAPP, ChannelType.OTHER), state.channels)
    assertEquals(ChannelType.OTHER, state.lastChannel)
    assertEquals("Signal", state.otherHandle)
  }

  @Test
  fun aProtectedPersonIsSelectableButExposesNoChannelsNorLastChannel() = runTest {
    val luna = db.addPerson("Luna", isProtected = true)
    db.addChannel(luna.id, ChannelType.DISCORD)
    db.addEntry(luna.id)
    val vm = viewModel(luna.id)

    val state = vm.uiState.value

    assertEquals(luna.id, state.selected?.id)
    assertTrue(state.channels.isEmpty())
    assertNull(state.lastChannel)
  }

  @Test
  fun startsWithTheGivenPerson() = runTest {
    val marta = db.addPerson("Marta")

    assertEquals(marta.id, viewModel(marta.id).uiState.value.selected?.id)
  }

  @Test
  fun aPersonPostedOnTheBridgeIsSelectedAndTheBridgeIsCleared() = runTest {
    val marta = db.addPerson("Marta")
    val vm = viewModel()

    bridge.post(marta.id)

    assertEquals(marta.id, vm.uiState.value.selected?.id)
    assertNull(bridge.picked.value)
  }

  @Test
  fun savingStoresTheEntryAndReports() = runTest {
    val marta = db.addPerson("Marta")
    val vm = viewModel(marta.id)
    var saved = false

    vm.save(marta.id, EntryKind.HEARD, ChannelType.DISCORD, "hola", occurredAt = 4_000) { saved = true }

    assertTrue(saved)
    val entry = db.repository.getEntries(marta.id).single()
    assertEquals(EntryKind.HEARD, entry.kind)
    assertEquals(ChannelType.DISCORD, entry.channelType)
    assertEquals("hola", entry.text)
    assertEquals(4_000, entry.occurredAt)
  }
}

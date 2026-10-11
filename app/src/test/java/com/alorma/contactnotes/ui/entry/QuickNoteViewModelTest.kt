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
  fun startsWithoutAPersonSelected() =
    runTest {
      db.addPerson("Marta")

      val state = viewModel().uiState.value

      assertNull(state.selected)
      assertEquals(1, state.people.size)
    }

  @Test
  fun selectingAPersonLoadsTheChannelOfTheLastEntry() =
    runTest {
      val marta = db.addPerson("Marta")
      db.addEntry(marta.id, channel = ChannelType.SOCIAL, occurredAt = 9_000)
      db.addEntry(marta.id, channel = ChannelType.CHAT, occurredAt = 1_000)
      val vm = viewModel()

      vm.select(marta)

      val state = vm.uiState.value
      assertEquals(marta.id, state.selected?.id)
      assertEquals(ChannelType.SOCIAL, state.lastChannel)
    }

  @Test
  fun selectingAProtectedPersonLeavesItPendingAndKeepsTheCurrentSelection() =
    runTest {
      val marta = db.addPerson("Marta")
      val luna = db.addPerson("Luna", isProtected = true)
      db.addEntry(luna.id)
      val vm = viewModel(marta.id)

      vm.select(luna)

      val state = vm.uiState.value
      assertEquals(marta.id, state.selected?.id)
      assertEquals(luna.id, state.pending?.id)
    }

  @Test
  fun aPendingProtectedPersonExposesNoLastChannelUntilItIsUnlocked() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)
      db.addEntry(luna.id, channel = ChannelType.CHAT)
      val vm = viewModel()

      vm.select(luna)

      val state = vm.uiState.value
      assertNull(state.selected)
      assertNull(state.lastChannel)
    }

  @Test
  fun unlockingSelectsTheProtectedPersonAndLoadsItsLastChannel() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)
      db.addEntry(luna.id, channel = ChannelType.CHAT, occurredAt = 1_000)
      db.addEntry(luna.id, channel = ChannelType.SOCIAL, occurredAt = 2_000)
      val vm = viewModel()
      vm.select(luna)

      vm.unlock(luna.id)

      val state = vm.uiState.value
      assertEquals(luna.id, state.selected?.id)
      assertNull(state.pending)
      assertEquals(ChannelType.SOCIAL, state.lastChannel)
    }

  @Test
  fun cancelingTheAuthenticationKeepsTheSelectionAsItWas() =
    runTest {
      val marta = db.addPerson("Marta")
      val luna = db.addPerson("Luna", isProtected = true)
      val vm = viewModel(marta.id)
      vm.select(luna)

      vm.cancelPending()

      assertEquals(
        marta.id,
        vm.uiState.value.selected
          ?.id,
      )
      assertNull(vm.uiState.value.pending)
    }

  @Test
  fun anUnlockedProtectedPersonIsNotAskedAgainWhenPickedAgain() =
    runTest {
      val marta = db.addPerson("Marta")
      val luna = db.addPerson("Luna", isProtected = true)
      val vm = viewModel()
      vm.select(luna)
      vm.unlock(luna.id)
      vm.select(marta)

      vm.select(luna)

      assertEquals(
        luna.id,
        vm.uiState.value.selected
          ?.id,
      )
      assertNull(vm.uiState.value.pending)
    }

  @Test
  fun openingWithAProtectedPersonLeavesItPendingInsteadOfSelected() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)

      val state = viewModel(luna.id).uiState.value

      assertNull(state.selected)
      assertEquals(luna.id, state.pending?.id)
    }

  @Test
  fun aProtectedPersonPostedOnTheBridgeIsPendingToo() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)
      val vm = viewModel()

      bridge.post(luna.id)

      assertNull(vm.uiState.value.selected)
      assertEquals(
        luna.id,
        vm.uiState.value.pending
          ?.id,
      )
      assertNull(bridge.picked.value)
    }

  @Test
  fun aProtectedPersonCreatedJustNowIsSelectedWithoutAskingAgain() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)
      val vm = viewModel()

      bridge.post(luna.id, authenticated = true)

      assertEquals(
        luna.id,
        vm.uiState.value.selected
          ?.id,
      )
      assertNull(vm.uiState.value.pending)
      assertNull(bridge.picked.value)
    }

  @Test
  fun startsWithTheGivenPerson() =
    runTest {
      val marta = db.addPerson("Marta")

      assertEquals(
        marta.id,
        viewModel(marta.id)
          .uiState.value.selected
          ?.id,
      )
    }

  @Test
  fun aPersonPostedOnTheBridgeIsSelectedAndTheBridgeIsCleared() =
    runTest {
      val marta = db.addPerson("Marta")
      val vm = viewModel()

      bridge.post(marta.id)

      assertEquals(
        marta.id,
        vm.uiState.value.selected
          ?.id,
      )
      assertNull(bridge.picked.value)
    }

  @Test
  fun savingStoresTheEntryAndReports() =
    runTest {
      val marta = db.addPerson("Marta")
      val vm = viewModel(marta.id)
      var saved = false

      vm.save(marta.id, EntryKind.HEARD, ChannelType.CHAT, "hola", occurredAt = 4_000) { saved = true }

      assertTrue(saved)
      val entry = db.repository.getEntries(marta.id).single()
      assertEquals(EntryKind.HEARD, entry.kind)
      assertEquals(ChannelType.CHAT, entry.channelType)
      assertEquals("hola", entry.text)
      assertEquals(4_000, entry.occurredAt)
    }
}

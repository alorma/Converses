package com.alorma.contactnotes.ui.person

import android.content.ContextWrapper
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()
  private val person = db.addPerson("Marta Soler")

  private fun at(day: Int, hour: Int): Long =
    LocalDate.of(2025, 3, day).atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

  private fun kotlinx.coroutines.test.TestScope.viewModel(): PersonDetailViewModel {
    val vm = PersonDetailViewModel(person.id, db.repository, PersonExporter(ContextWrapper(null), db.repository))
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    return vm
  }

  @Test
  fun timelineIsNewestFirstAndCountsAreByKind() = runTest {
    db.addEntry(person.id, EntryKind.SAID, text = "first", occurredAt = at(1, 10))
    db.addEntry(person.id, EntryKind.HEARD, text = "second", occurredAt = at(2, 10))
    db.addEntry(person.id, EntryKind.HEARD, text = "third", occurredAt = at(3, 10))
    val vm = viewModel()

    val state = vm.uiState.value
    assertEquals(listOf("third", "second", "first"), state.entries.map { it.text })
    assertEquals(1, state.saidCount)
    assertEquals(2, state.heardCount)
    assertEquals(0, state.happenedCount)
    assertEquals(2, state.countOf(EntryKind.HEARD))
  }

  @Test
  fun channelsAreDerivedFromTheEntriesInTheFixedOrderWithoutRepeats() = runTest {
    db.addEntry(person.id, channel = ChannelType.SOCIAL, occurredAt = at(1, 10))
    db.addEntry(person.id, channel = ChannelType.CHAT, occurredAt = at(2, 10))
    db.addEntry(person.id, channel = ChannelType.IN_PERSON, occurredAt = at(3, 10))
    db.addEntry(person.id, channel = ChannelType.CHAT, occurredAt = at(4, 10))
    val vm = viewModel()

    assertEquals(listOf(ChannelType.IN_PERSON, ChannelType.CHAT, ChannelType.SOCIAL), vm.uiState.value.channels)
  }

  @Test
  fun channelsStayEmptyWithoutEntriesEvenIfOldChannelRowsExist() = runTest {
    db.addChannel(person.id, ChannelType.PHONE)
    val vm = viewModel()

    assertTrue(vm.uiState.value.channels.isEmpty())
  }

  @Test
  fun channelsFollowTheEntriesWhenOneIsAdded() = runTest {
    val vm = viewModel()
    assertTrue(vm.uiState.value.channels.isEmpty())

    vm.addEntry(EntryKind.SAID, ChannelType.PHONE, "call", occurredAt = null)

    assertEquals(listOf(ChannelType.PHONE), vm.uiState.value.channels)
  }

  @Test
  fun togglingAKindFiltersTheTimelineButNotTheCounts() = runTest {
    db.addEntry(person.id, EntryKind.SAID, text = "said")
    db.addEntry(person.id, EntryKind.HEARD, text = "heard")
    val vm = viewModel()

    vm.onKindToggled(EntryKind.SAID)
    assertEquals(listOf("heard"), vm.uiState.value.entries.map { it.text })
    assertEquals(1, vm.uiState.value.saidCount)

    vm.onKindToggled(EntryKind.SAID)
    assertEquals(2, vm.uiState.value.entries.size)
  }

  @Test
  fun deselectingEveryKindLeavesTheTimelineEmpty() = runTest {
    db.addEntry(person.id, EntryKind.SAID)
    val vm = viewModel()

    EntryKind.entries.forEach { vm.onKindToggled(it) }

    assertTrue(vm.uiState.value.entries.isEmpty())
    assertTrue(vm.uiState.value.selectedKinds.isEmpty())
  }

  @Test
  fun addEntryUsesTheGivenTimeOrNow() = runTest {
    val vm = viewModel()
    val before = System.currentTimeMillis()

    vm.addEntry(EntryKind.HAPPENED, ChannelType.IN_PERSON, "met", occurredAt = null)
    vm.addEntry(EntryKind.SAID, ChannelType.PHONE, "called", occurredAt = 5_000)

    val entries = vm.uiState.value.entries.associateBy { it.text }
    assertTrue(entries.getValue("met").occurredAt >= before)
    assertEquals(5_000, entries.getValue("called").occurredAt)
  }

  @Test
  fun updateEntryKeepsTheDateWhenNoneIsGiven() = runTest {
    val entry = db.addEntry(person.id, occurredAt = 7_000)
    val vm = viewModel()

    vm.updateEntry(entry, EntryKind.HEARD, ChannelType.SOCIAL, "edited", occurredAt = null)

    val updated = vm.uiState.value.entries.single()
    assertEquals("edited", updated.text)
    assertEquals(EntryKind.HEARD, updated.kind)
    assertEquals(7_000, updated.occurredAt)
  }

  @Test
  fun deleteAndRestoreEntryRoundTrips() = runTest {
    val entry = db.addEntry(person.id, text = "keep me")
    val vm = viewModel()

    vm.deleteEntry(entry)
    assertTrue(vm.uiState.value.entries.isEmpty())

    vm.restoreEntry(entry)
    assertEquals(listOf("keep me"), vm.uiState.value.entries.map { it.text })
  }

  @Test
  fun deleteDayOnlyRemovesThatDayAndChannel() = runTest {
    val target = db.addEntry(person.id, channel = ChannelType.CHAT, text = "a", occurredAt = at(5, 9))
    db.addEntry(person.id, channel = ChannelType.CHAT, text = "b", occurredAt = at(5, 21))
    db.addEntry(person.id, channel = ChannelType.SOCIAL, text = "other channel", occurredAt = at(5, 12))
    db.addEntry(person.id, channel = ChannelType.CHAT, text = "other day", occurredAt = at(6, 9))
    val vm = viewModel()

    assertEquals(2, vm.countDay(target))
    vm.deleteDay(target)

    assertEquals(setOf("other channel", "other day"), vm.uiState.value.entries.map { it.text }.toSet())
  }

  @Test
  fun moveEntryChangesItsOwner() = runTest {
    val other = db.addPerson("Jordi")
    val entry = db.addEntry(person.id)
    val vm = viewModel()

    vm.moveEntry(entry, other.id)

    assertTrue(vm.uiState.value.entries.isEmpty())
    assertEquals(1, db.repository.getEntries(other.id).size)
  }

  @Test
  fun archivingReportsBackWhenDone() = runTest {
    val vm = viewModel()
    var done = false

    vm.setArchived(true) { done = true }

    assertTrue(done)
    assertTrue(db.person(person.id)!!.archived)
  }
}

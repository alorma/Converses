package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.wear.data.DiscardedNotes
import com.alorma.contactnotes.wear.data.PhoneConnection
import com.alorma.contactnotes.wear.data.StoredSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
  private val now = 10_000_000L
  private val shape = AvatarShape.entries.first()

  private val labels =
    object : WearLabels {
      override val me = "Jo"
      override val note = "Apunt"

      override fun channel(type: ChannelType) = type.name

      override fun relative(time: RelativeTime) = "rel"

      override fun clock(timestamp: Long) = "21:40"
    }

  private class FakeDiscarded(
    initial: Int = 0,
  ) : DiscardedNotes {
    val flow = MutableStateFlow(initial)
    override val count: Flow<Int> = flow

    override suspend fun add(amount: Int) {
      flow.value += amount
    }

    override suspend fun clear() {
      flow.value = 0
    }
  }

  private val discarded = FakeDiscarded()

  private class FakePhone(
    initial: Boolean,
  ) : PhoneConnection {
    val flow = MutableStateFlow(initial)
    override val connected: Flow<Boolean> = flow
  }

  private val kael =
    WearPerson(
      1,
      "Kael",
      AvatarColor.Sky,
      shape,
      isProtected = false,
      lastEntryAt = now,
      entries = listOf(WearEntry(5, EntryKind.HEARD, ChannelType.CHAT, "hola", now)),
    )
  private val marta = WearPerson(2, "Marta", AvatarColor.Peach, shape, isProtected = true)

  private fun stored(
    vararg people: WearPerson,
    syncedAt: Long = now,
  ) = StoredSnapshot(WearSnapshot(generatedAt = now, appLockActive = false, people = people.toList()), syncedAt)

  @Before
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun peopleStartsLoadingThenNoData() =
    runTest {
      val vm = PeopleViewModel(MutableStateFlow(null), FakePhone(true), labels, discarded) { now }
      assertEquals(PeopleUiState.Loading, vm.state.value)
      assertEquals(PeopleUiState.NoData, vm.state.first { it != PeopleUiState.Loading })
    }

  @Test
  fun peopleConnectedHasNoAgeBanner() =
    runTest {
      val vm = PeopleViewModel(MutableStateFlow(stored(kael, marta)), FakePhone(true), labels, discarded) { now }
      val loaded = vm.state.first { it is PeopleUiState.Loaded } as PeopleUiState.Loaded
      assertNull(loaded.dataAgeMinutes)
      assertEquals(listOf("Kael", "Marta"), loaded.people.map { it.name })
      assertNull(loaded.people.last().lastEntry)
    }

  @Test
  fun peopleShowsDiscardedNotesUntilDismissed() =
    runTest {
      val notes = FakeDiscarded(initial = 2)
      val vm = PeopleViewModel(MutableStateFlow(stored(kael)), FakePhone(true), labels, notes) { now }
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
      assertEquals(2, (vm.state.value as PeopleUiState.Loaded).discardedNotes)

      vm.dismissDiscardedNotes()

      assertEquals(0, (vm.state.value as PeopleUiState.Loaded).discardedNotes)
    }

  @Test
  fun peopleOfflineShowsDataAge() =
    runTest {
      val vm =
        PeopleViewModel(
          MutableStateFlow(stored(kael, syncedAt = now - 14 * 60_000L)),
          FakePhone(false),
          labels,
          discarded,
        ) {
          now
        }
      val loaded = vm.state.first { it is PeopleUiState.Loaded } as PeopleUiState.Loaded
      assertEquals(14, loaded.dataAgeMinutes)
    }

  @Test
  fun peopleReactsToConnectionChanges() =
    runTest {
      val phone = FakePhone(true)
      val vm = PeopleViewModel(MutableStateFlow(stored(kael)), phone, labels, discarded) { now }
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
      phone.flow.value = false
      assertTrue((vm.state.value as PeopleUiState.Loaded).dataAgeMinutes != null)
    }

  @Test
  fun timelineShowsEntriesOfPerson() =
    runTest {
      val vm = TimelineViewModel(1, MutableStateFlow(stored(kael, marta)), labels, NoMoreEntries)
      val loaded = vm.state.first { it is TimelineUiState.Loaded } as TimelineUiState.Loaded
      assertEquals("Kael", loaded.name)
      assertEquals(1, loaded.entries.size)
    }

  @Test
  fun timelineOfProtectedPersonIsUnavailable() =
    runTest {
      val vm = TimelineViewModel(2, MutableStateFlow(stored(kael, marta)), labels, NoMoreEntries)
      assertEquals(TimelineUiState.Unavailable, vm.state.first { it != TimelineUiState.Loading })
    }

  @Test
  fun timelineOfMissingPersonIsUnavailable() =
    runTest {
      val vm = TimelineViewModel(99, MutableStateFlow(stored(kael)), labels, NoMoreEntries)
      assertEquals(TimelineUiState.Unavailable, vm.state.first { it != TimelineUiState.Loading })
    }
}

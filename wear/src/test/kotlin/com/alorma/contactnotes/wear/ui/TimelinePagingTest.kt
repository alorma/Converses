package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WEAR_MAX_EXTRA_ENTRIES
import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.wear.data.MoreEntriesResult
import com.alorma.contactnotes.wear.data.MoreEntriesSource
import com.alorma.contactnotes.wear.data.StoredSnapshot
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fals que no té res més al mòbil. */
object NoMoreEntries : MoreEntriesSource {
  override suspend fun load(
    personId: Long,
    beforeOccurredAt: Long,
    beforeId: Long,
  ) = MoreEntriesResult.Unavailable
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelinePagingTest {
  private val labels =
    object : WearLabels {
      override val me = "Jo"
      override val note = "Apunt"

      override fun channel(type: ChannelType) = type.name

      override fun relative(time: RelativeTime) = "rel"

      override fun clock(timestamp: Long) = "21:40"
    }

  private class FakeSource(
    val reply: suspend (cursor: Long) -> MoreEntriesResult,
  ) : MoreEntriesSource {
    val cursors = mutableListOf<Long>()

    override suspend fun load(
      personId: Long,
      beforeOccurredAt: Long,
      beforeId: Long,
    ): MoreEntriesResult {
      cursors += beforeOccurredAt
      return reply(beforeOccurredAt)
    }
  }

  private fun entry(at: Long) = WearEntry(at, EntryKind.SAID, ChannelType.CHAT, "e$at", at)

  /** Entrades amb `occurredAt` de [from] cap avall, [count] en total (la més nova primer). */
  private fun descending(
    from: Long,
    count: Int,
  ) = (0 until count).map { entry(from - it) }

  private fun person(entries: List<WearEntry>) =
    WearPerson(
      1,
      "Kael",
      AvatarColor.Sky,
      AvatarShape.entries.first(),
      false,
      entries.firstOrNull()?.occurredAt,
      entries,
    )

  private fun snapshots(person: WearPerson) =
    MutableStateFlow<StoredSnapshot?>(
      StoredSnapshot(WearSnapshot(generatedAt = 1, appLockActive = false, people = listOf(person)), 1),
    )

  private fun viewModel(
    person: WearPerson,
    source: MoreEntriesSource,
  ) = TimelineViewModel(1, snapshots(person), labels, source)

  private suspend fun TimelineViewModel.loaded() =
    state.first { it is TimelineUiState.Loaded } as TimelineUiState.Loaded

  @Before
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun loadMoreAppendsThePageAfterTheLocalCopyUsingTheLastEntryAsCursor() =
    runTest {
      val source = FakeSource { MoreEntriesResult.Page(descending(it - 1, 20), hasMore = true) }
      val vm = viewModel(person(descending(100, 5)), source)
      vm.loaded()
      vm.loadMore()
      assertEquals(listOf(96L), source.cursors)
      assertEquals(25, vm.loaded().entries.size)
      assertEquals(95L, vm.loaded().entries[5].id)
      assertFalse(vm.loaded().loadingMore)
    }

  @Test
  fun showsTheLoadingIndicatorWhileWaitingAndIgnoresRepeatedRequests() =
    runTest {
      val gate = CompletableDeferred<MoreEntriesResult>()
      val source = FakeSource { gate.await() }
      val vm = viewModel(person(descending(100, 5)), source)
      vm.loaded()
      vm.loadMore()
      vm.loadMore()
      assertTrue(vm.loaded().loadingMore)
      assertEquals(1, source.cursors.size)
      gate.complete(MoreEntriesResult.Page(descending(95, 3), hasMore = false))
      assertFalse(vm.loaded().loadingMore)
      assertEquals(8, vm.loaded().entries.size)
    }

  @Test
  fun stopsAskingAtTheEndOfTheList() =
    runTest {
      val source = FakeSource { MoreEntriesResult.Page(descending(it - 1, 3), hasMore = false) }
      val vm = viewModel(person(descending(100, 5)), source)
      vm.loaded()
      vm.loadMore()
      vm.loadMore()
      assertEquals(1, source.cursors.size)
      assertEquals(8, vm.loaded().entries.size)
    }

  @Test
  fun unavailablePhoneEndsTheListWithoutErrorAndKeepsTheLocalEntries() =
    runTest {
      val source = FakeSource { MoreEntriesResult.Unavailable }
      val vm = viewModel(person(descending(100, 5)), source)
      vm.loaded()
      vm.loadMore()
      vm.loadMore()
      assertEquals(1, source.cursors.size)
      assertEquals(5, vm.loaded().entries.size)
      assertFalse(vm.loaded().loadingMore)
    }

  @Test
  fun doesNotAskWhenTheCopyHoldsFewerEntriesThanTheMaximum() =
    runTest {
      val source = FakeSource { error("no s'havia de demanar") }
      val vm = viewModel(person(descending(100, 3)), source)
      vm.loaded()
      vm.loadMore()
      assertTrue(source.cursors.isEmpty())
      assertEquals(3, vm.loaded().entries.size)
    }

  @Test
  fun memoryIsCappedAtTheExtraEntriesLimit() =
    runTest {
      val source = FakeSource { MoreEntriesResult.Page(descending(it - 1, 20), hasMore = true) }
      val vm = viewModel(person(descending(10_000, 5)), source)
      vm.loaded()
      repeat(10) { vm.loadMore() }
      assertEquals(5 + WEAR_MAX_EXTRA_ENTRIES, vm.loaded().entries.size)
      assertEquals(WEAR_MAX_EXTRA_ENTRIES / 20, source.cursors.size)
    }

  @Test
  fun overlappingEntriesAreNotRepeated() =
    runTest {
      val source = FakeSource { MoreEntriesResult.Page(descending(it, 4), hasMore = false) }
      val vm = viewModel(person(descending(100, 5)), source)
      vm.loaded()
      vm.loadMore()
      val ids = vm.loaded().entries.map { it.id }
      assertEquals(ids.distinct(), ids)
    }

  @Test
  fun moreEntriesNeverReachTheDisk() {
    val sources = File("src/main/kotlin").walkTopDown().filter { it.name == "TimelineViewModel.kt" }.toList()
    val text = sources.single().readText()
    listOf("DataStore", "SharedPreferences", "File(", "WearSnapshotStore", "Room").forEach {
      assertFalse("TimelineViewModel no ha de tocar el disc: $it", it in text)
    }
  }
}

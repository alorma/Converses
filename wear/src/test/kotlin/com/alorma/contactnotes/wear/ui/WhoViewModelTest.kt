package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearNote
import com.alorma.contactnotes.shared.WearPerson
import com.alorma.contactnotes.shared.WearSnapshot
import com.alorma.contactnotes.wear.data.NoteDelivery
import com.alorma.contactnotes.wear.data.NoteSender
import com.alorma.contactnotes.wear.data.NoteSubmitter
import com.alorma.contactnotes.wear.data.PhoneConnection
import com.alorma.contactnotes.wear.data.ProtectedSession
import com.alorma.contactnotes.wear.data.ProtectedSessions
import com.alorma.contactnotes.wear.data.StoredSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class WhoViewModelTest {
  private class FakePhone(
    connected: Boolean,
  ) : PhoneConnection {
    override val connected: Flow<Boolean> = MutableStateFlow(connected)
  }

  private val submitted = mutableListOf<WearNote>()
  private val outbox = NoteSubmitter { submitted += it }

  private fun person(
    id: Long = 1,
    isProtected: Boolean = false,
  ) = WearPerson(
    id,
    "Kael Rojas",
    AvatarColor.Sky,
    AvatarShape.entries.first(),
    isProtected = isProtected,
    lastEntryAt = 0,
  )

  private fun snapshot(vararg people: WearPerson): Flow<StoredSnapshot?> =
    MutableStateFlow(
      StoredSnapshot(WearSnapshot(generatedAt = 0, appLockActive = false, people = people.toList()), syncedAt = 0),
    )

  private fun viewModel(
    people: Flow<StoredSnapshot?> = snapshot(person()),
    connected: Boolean = true,
    text: String = "  Li va agradar el llibre  ",
  ) = WhoViewModel(1, text, people, outbox, FakePhone(connected), clock = { 42L }, newId = { "uuid-1" })

  @Before
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun startsOnThePersonWithTheirNameAndInitials() =
    runTest {
      val vm = viewModel()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
      val state = vm.state.value as WhoUiState.Choosing
      assertEquals(1, state.selectedIndex)
      assertEquals("Kael Rojas", state.name)
      assertEquals("KR", state.initials)
    }

  @Test
  fun savesTheNoteWithTheChosenKindAndTrimmedText() =
    runTest {
      val vm = viewModel()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
      vm.select(0)
      vm.save()
      val note = submitted.single()
      assertEquals(EntryKind.SAID, note.who)
      assertEquals("Li va agradar el llibre", note.text)
      assertEquals("uuid-1", note.id)
      assertEquals(1L, note.personId)
      assertEquals(42L, note.timestamp)
      assertEquals(WhoUiState.Saved(pendingPhone = false), vm.state.value)
    }

  @Test
  fun defaultChoiceIsHeardAndTheThirdIsHappened() =
    runTest {
      val first = viewModel()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.state.collect {} }
      first.save()
      val second = viewModel()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { second.state.collect {} }
      second.select(2)
      second.save()
      assertEquals(listOf(EntryKind.HEARD, EntryKind.HAPPENED), submitted.map { it.who })
    }

  @Test
  fun withoutPhoneTheSavedScreenSaysItIsPending() =
    runTest {
      val vm = viewModel(connected = false)
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
      vm.save()
      assertEquals(WhoUiState.Saved(pendingPhone = true), vm.state.value)
    }

  private val directSent = mutableListOf<WearNote>()
  private var directAnswer = NoteDelivery.Delivered
  private val sessions = ProtectedSessions()

  private fun protectedViewModel() =
    WhoViewModel(
      1,
      "  Secret  ",
      snapshot(person()),
      outbox,
      FakePhone(true),
      clock = { 42L },
      newId = { "uuid-1" },
      sessions = sessions,
      directSender =
        NoteSender {
          directSent += it
          directAnswer
        },
    ).also { vm ->
      sessions.open(ProtectedSession(1, emptyList(), hasMore = false, noteToken = "tok", noteTokenExpiresAt = 99))
      CoroutineScope(UnconfinedTestDispatcher()).launch { vm.state.collect {} }
    }

  @Test
  fun aNoteToAnOpenProtectedPersonGoesStraightToThePhoneWithTheTokenAndNeverToTheQueue() =
    runTest {
      val vm = protectedViewModel()

      vm.save()

      assertEquals("tok", directSent.single().approvalToken)
      assertEquals("Secret", directSent.single().text)
      assertTrue(submitted.isEmpty())
      assertEquals(WhoUiState.Saved(pendingPhone = false), vm.state.value)
    }

  @Test
  fun aProtectedNoteWithoutPhoneIsNotSavedAnywhere() =
    runTest {
      directAnswer = NoteDelivery.Unavailable
      val vm = protectedViewModel()

      vm.save()

      assertEquals(WhoUiState.NotSent(phoneMissing = true), vm.state.value)
      assertTrue(submitted.isEmpty())
    }

  @Test
  fun aProtectedNoteRejectedByThePhoneIsNotSavedEither() =
    runTest {
      directAnswer = NoteDelivery.Rejected
      val vm = protectedViewModel()

      vm.save()

      assertEquals(WhoUiState.NotSent(phoneMissing = false), vm.state.value)
      assertTrue(submitted.isEmpty())
    }

  @Test
  fun aNormalPersonStillUsesTheQueueAndSendsNoToken() =
    runTest {
      val vm = viewModel()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

      vm.save()

      assertNull(submitted.single().approvalToken)
      assertTrue(directSent.isEmpty())
    }

  @Test
  fun aSecondTapDoesNotQueueTheNoteTwice() =
    runTest {
      val vm = viewModel()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
      vm.save()
      vm.save()
      assertEquals(1, submitted.size)
    }

  @Test
  fun aProtectedOrMissingPersonCannotReceiveANote() =
    runTest {
      val protectedVm = viewModel(people = snapshot(person(isProtected = true)))
      val missingVm = viewModel(people = snapshot(person(id = 2)))
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { protectedVm.state.collect {} }
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { missingVm.state.collect {} }
      protectedVm.save()
      missingVm.save()
      assertEquals(WhoUiState.Unavailable, protectedVm.state.value)
      assertEquals(WhoUiState.Unavailable, missingVm.state.value)
      assertTrue(submitted.isEmpty())
    }
}

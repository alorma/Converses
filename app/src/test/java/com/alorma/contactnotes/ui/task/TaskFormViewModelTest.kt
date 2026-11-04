package com.alorma.contactnotes.ui.task

import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()
  private val bridge = PersonSelectionBridge()
  private val tracker = RecordingTracker()

  @Test
  fun savingIsTrackedWithModeAndDueButNoText() =
    runTest {
      val marta = db.addPerson("Marta")
      val vm = viewModel(marta.id)
      vm.save(marta.id, "secret", LocalDate.of(2026, 11, 5)) {}

      assertEquals(
        listOf("task_saved" to mapOf("mode" to "create", "has_due" to "true")),
        tracker.recordedActions(),
      )
    }

  private fun TestScope.viewModel(
    personId: Long = 0L,
    taskId: Long = 0L,
  ): TaskFormViewModel {
    val vm = TaskFormViewModel(personId, taskId, db.repository, bridge, tracker.eventTracker, clock = { 5_000L })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    return vm
  }

  @Test
  fun theInitialPersonComesSelected() =
    runTest {
      val marta = db.addPerson("Marta")

      val state = viewModel(marta.id).uiState.value

      assertEquals(marta.id, state.selected?.id)
      assertNull(state.task)
      assertTrue(state.loaded)
    }

  @Test
  fun aProtectedInitialPersonComesFromAnUnlockedDetailAndIsSelected() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)

      val state = viewModel(luna.id).uiState.value

      assertEquals(luna.id, state.selected?.id)
      assertNull(state.pending)
    }

  @Test
  fun pickingAnotherProtectedPersonAsksToUnlockIt() =
    runTest {
      val marta = db.addPerson("Marta")
      val luna = db.addPerson("Luna", isProtected = true)
      val vm = viewModel(marta.id)

      vm.select(luna)
      assertEquals(
        marta.id,
        vm.uiState.value.selected
          ?.id,
      )
      assertEquals(
        luna.id,
        vm.uiState.value.pending
          ?.id,
      )

      vm.unlock(luna.id)
      assertEquals(
        luna.id,
        vm.uiState.value.selected
          ?.id,
      )
      assertNull(vm.uiState.value.pending)
    }

  @Test
  fun savingANewTaskStoresTheTrimmedTextTheDayAndTheCreationTime() =
    runTest {
      val marta = db.addPerson("Marta")
      val vm = viewModel(marta.id)
      var saved = false

      vm.save(marta.id, "  Tornar-li el llibre ", LocalDate.of(2026, 11, 6), onSaved = { saved = true })

      val task = db.tasks.value.single()
      assertTrue(saved)
      assertEquals("Tornar-li el llibre", task.text)
      assertEquals(LocalDate.of(2026, 11, 6).toEpochDay(), task.dueDate)
      assertEquals(5_000L, task.createdAt)
      assertNull(task.doneAt)
    }

  @Test
  fun aBlankTextIsNotSaved() =
    runTest {
      val marta = db.addPerson("Marta")

      viewModel(marta.id).save(marta.id, "   ", null, onSaved = {})

      assertTrue(db.tasks.value.isEmpty())
    }

  @Test
  fun editingLoadsTheTaskAndKeepsItsCreationAndDoneTime() =
    runTest {
      val marta = db.addPerson("Marta")
      val jordi = db.addPerson("Jordi")
      val task = db.addTask(marta.id, text = "Trucar", dueDate = 10, createdAt = 1_000, doneAt = 2_000)
      val vm = viewModel(taskId = task.id)

      val state = vm.uiState.value
      assertEquals(task, state.task)
      assertEquals(marta.id, state.selected?.id)

      vm.save(jordi.id, "Trucar-lo", null, onSaved = {})

      val updated = db.tasks.value.single()
      assertEquals(task.id, updated.id)
      assertEquals(jordi.id, updated.personId)
      assertEquals("Trucar-lo", updated.text)
      assertNull(updated.dueDate)
      assertEquals(1_000L, updated.createdAt)
      assertEquals(2_000L, updated.doneAt)
    }

  @Test
  fun editingATaskOfAProtectedPersonSelectsItWithoutAskingAgain() =
    runTest {
      val luna = db.addPerson("Luna", isProtected = true)
      val task = db.addTask(luna.id)

      val state = viewModel(taskId = task.id).uiState.value

      assertEquals(luna.id, state.selected?.id)
      assertNull(state.pending)
    }

  @Test
  fun aMissingTaskClosesTheForm() =
    runTest {
      db.addPerson("Marta")

      val state = viewModel(taskId = 99).uiState.value

      assertTrue(state.missing)
      assertFalse(state.loaded)
    }

  @Test
  fun deletingRemovesTheTask() =
    runTest {
      val marta = db.addPerson("Marta")
      val task = db.addTask(marta.id)
      val vm = viewModel(taskId = task.id)
      var deleted = false

      vm.delete(onDeleted = { deleted = true })

      assertTrue(deleted)
      assertTrue(db.tasks.value.isEmpty())
      assertEquals("task_deleted", tracker.recordedActions().last().first)
    }

  @Test
  fun aPersonCreatedFromThePickerIsSelected() =
    runTest {
      val marta = db.addPerson("Marta")
      val vm = viewModel(marta.id)
      val nova = db.addPerson("Nova", isProtected = true)

      bridge.post(nova.id, authenticated = true)

      assertEquals(
        nova.id,
        vm.uiState.value.selected
          ?.id,
      )
    }
}

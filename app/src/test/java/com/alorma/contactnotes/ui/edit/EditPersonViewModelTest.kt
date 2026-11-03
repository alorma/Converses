package com.alorma.contactnotes.ui.edit

import android.content.ContextWrapper
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.ui.person.PersonExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditPersonViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()

  private fun viewModel(personId: Long = 0L) =
    EditPersonViewModel(
      personId,
      db.repository,
      PersonExporter(ContextWrapper(null), db.repository),
    )

  @Test
  fun aNewPersonNeedsANameToBeSaved() {
    val vm = viewModel()
    assertTrue(vm.state.value.isNew)
    assertFalse(vm.state.value.canSave)

    vm.onName("  ")
    assertFalse(vm.state.value.canSave)

    vm.onName("Marta")
    assertTrue(vm.state.value.canSave)
  }

  @Test
  fun aliasTakenByAnotherPersonBlocksSavingIgnoringCaseAndTheAtSign() {
    db.addPerson("Marta", alias = "marta")
    val vm = viewModel()
    vm.onName("Other")

    vm.onAlias("@MARTA")

    assertEquals("MARTA", vm.state.value.alias)
    assertTrue(vm.state.value.aliasTaken)
    assertFalse(vm.state.value.canSave)

    vm.onAlias("free")
    assertFalse(vm.state.value.aliasTaken)
    assertTrue(vm.state.value.canSave)
  }

  @Test
  fun anExistingPersonCanKeepItsOwnAlias() {
    val marta = db.addPerson("Marta", alias = "marta")
    val vm = viewModel(marta.id)

    vm.onAlias("marta")

    assertFalse(vm.state.value.aliasTaken)
  }

  @Test
  fun anExistingPersonOnlyCanBeSavedOnceSomethingChanged() {
    val marta = db.addPerson("Marta")
    val vm = viewModel(marta.id)
    assertTrue(vm.state.value.loaded)
    assertFalse(vm.state.value.isNew)
    assertFalse(vm.state.value.canSave)

    vm.onName("Marta Soler")
    assertTrue(vm.state.value.canSave)

    vm.onName("Marta")
    assertFalse(vm.state.value.canSave)
  }

  @Test
  fun savingANewPersonStoresTrimmedDataAndDropsBlankFacts() =
    runTest {
      val vm = viewModel()
      vm.onName("  Marta  ")
      vm.onAlias("m")
      vm.onAddFact()
      vm.onAddFact()
      val (first, second) = vm.state.value.facts
      vm.onFactChange(first.key, " Birthday ", " 14 March ")
      vm.onFactChange(second.key, "", "")

      var savedId = -1L
      vm.save { savedId = it }

      val person = db.person(savedId)!!
      assertEquals("Marta", person.name)
      assertEquals("m", person.alias)
      assertNull(person.avatarShape)
      assertNull(person.avatarColor)
      val facts = db.repository.observePinnedFacts(savedId).first()
      assertEquals(listOf("Birthday" to "14 March"), facts.map { it.label to it.value })
    }

  @Test
  fun factsCanBeReorderedAndOutOfRangeMovesAreIgnored() {
    val vm = viewModel()
    repeat(3) { vm.onAddFact() }
    vm.state.value.facts
      .forEachIndexed { i, fact -> vm.onFactChange(fact.key, "f$i", "v$i") }

    vm.onMoveFact(0, 2)
    assertEquals(
      listOf("f1", "f2", "f0"),
      vm.state.value.facts
        .map { it.label },
    )

    vm.onMoveFact(0, 9)
    vm.onMoveFact(-1, 1)
    assertEquals(
      listOf("f1", "f2", "f0"),
      vm.state.value.facts
        .map { it.label },
    )

    vm.onRemoveFact(
      vm.state.value.facts
        .first()
        .key,
    )

    assertEquals(2, vm.state.value.facts.size)
  }

  @Test
  fun protectingAnExistingPersonIsStoredAtOnceWithoutMakingTheFormDirty() {
    val marta = db.addPerson("Marta")
    val vm = viewModel(marta.id)

    vm.onProtected(true)

    assertTrue(db.person(marta.id)!!.isProtected)
    assertTrue(vm.state.value.isProtected)
    assertFalse(vm.state.value.canSave)

    vm.onProtected(false)
    assertFalse(db.person(marta.id)!!.isProtected)
  }

  @Test
  fun protectingANewPersonOnlyChangesTheFormUntilItIsSaved() {
    val vm = viewModel()
    vm.onName("Luna")

    vm.onProtected(true)

    assertTrue(vm.state.value.isProtected)
    assertNull(db.people.value.firstOrNull())

    var id = 0L
    vm.save { id = it }
    assertTrue(db.person(id)!!.isProtected)
  }

  @Test
  fun savingAnExistingPersonKeepsArchivedAndCreationDate() {
    val marta = db.addPerson("Marta", archived = true, createdAt = 123)
    val vm = viewModel(marta.id)
    vm.onName("Marta S")

    vm.save { }

    val saved = db.person(marta.id)!!
    assertEquals("Marta S", saved.name)
    assertTrue(saved.archived)
    assertEquals(123, saved.createdAt)
  }

  @Test
  fun saveIsIgnoredWhileTheFormIsNotValid() {
    val vm = viewModel()
    var called = false

    vm.save { called = true }

    assertFalse(called)
    assertTrue(db.people.value.isEmpty())
  }

  @Test
  fun deletingRemovesThePersonAndReports() {
    val marta = db.addPerson("Marta")
    db.addEntry(marta.id)
    val vm = viewModel(marta.id)
    var deleted = false

    vm.delete(exportFirst = false) { deleted = true }

    assertTrue(deleted)
    assertNull(db.person(marta.id))
    assertNotNull(vm.state.value)
  }

  @Test
  fun entryCountIsLoadedForExistingPeople() {
    val marta = db.addPerson("Marta")
    db.addEntry(marta.id)
    db.addEntry(marta.id)

    assertEquals(2, viewModel(marta.id).state.value.entryCount)
  }

  @Test
  fun aTrialIsShownButNeverSaved() {
    val marta = db.addPerson("Marta")
    val vm = viewModel(marta.id)

    vm.onTrialShape(AvatarShape.Clover4Leaf)
    vm.onTrialColor(AvatarColor.Sky)

    assertTrue(vm.state.value.hasTrial)
    assertEquals(AvatarShape.Cookie9Sided, vm.state.value.shape)
    assertEquals(marta.avatarColor, vm.state.value.color)
    assertFalse(vm.state.value.canSave)
  }

  @Test
  fun choosingAFreeOptionClearsTheTrialOfThatDimensionOnly() {
    val vm = viewModel()
    vm.onTrialShape(AvatarShape.Clover4Leaf)
    vm.onTrialColor(AvatarColor.Sky)

    vm.onShape(AvatarShape.Cookie9Sided)

    assertNull(vm.state.value.trialShape)
    assertEquals(AvatarColor.Sky, vm.state.value.trialColor)

    vm.onColor(null)

    assertFalse(vm.state.value.hasTrial)
  }

  @Test
  fun discardingTheTrialGoesBackToTheSavedAvatar() {
    val vm = viewModel()
    vm.onTrialShape(AvatarShape.Sunny)
    vm.onTrialColor(AvatarColor.Rose)

    vm.onDiscardTrial()

    assertNull(vm.state.value.trialShape)
    assertNull(vm.state.value.trialColor)
    assertNull(vm.state.value.shape)
    assertNull(vm.state.value.color)
  }

  @Test
  fun unlockingKeepsTheTrialAndReopensTheSheetOnce() {
    val vm = viewModel()
    vm.onTrialShape(AvatarShape.Sunny)

    vm.onUnlockTrial()

    assertTrue(vm.state.value.reopenAvatarSheet)
    assertEquals(AvatarShape.Sunny, vm.state.value.trialShape)

    vm.onAvatarSheetReopened()

    assertFalse(vm.state.value.reopenAvatarSheet)
    assertEquals(AvatarShape.Sunny, vm.state.value.trialShape)
  }

  @Test
  fun afterBuyingTheTrialIsAppliedAndCanBeSaved() =
    runTest {
      val marta = db.addPerson("Marta")
      val vm = viewModel(marta.id)
      vm.onTrialShape(AvatarShape.Clover4Leaf)
      vm.onTrialColor(AvatarColor.Sky)

      vm.onApplyTrial()

      assertFalse(vm.state.value.hasTrial)
      assertEquals(AvatarShape.Clover4Leaf, vm.state.value.shape)
      assertEquals(AvatarColor.Sky, vm.state.value.color)
      assertTrue(vm.state.value.canSave)

      vm.save {}

      assertEquals(AvatarShape.Clover4Leaf, db.person(marta.id)?.avatarShape)
      assertEquals(AvatarColor.Sky, db.person(marta.id)?.avatarColor)
    }

  @Test
  fun applyingOnlyAShapeTrialKeepsTheCurrentColor() {
    val vm = viewModel()
    vm.onColor(AvatarColor.Mint)
    vm.onTrialShape(AvatarShape.Ghostish)

    vm.onApplyTrial()

    assertEquals(AvatarShape.Ghostish, vm.state.value.shape)
    assertEquals(AvatarColor.Mint, vm.state.value.color)
  }

  @Test
  fun afterADowngradeChoosingCookieDropsTheProShapeOnSave() =
    runTest {
      val marta = db.addPerson("Marta")
      db.people.update { list ->
        list.map { it.copy(avatarShape = AvatarShape.Clover8Leaf, avatarColor = AvatarColor.Lavender) }
      }
      val vm = viewModel(marta.id)

      vm.onShape(AvatarShape.Cookie9Sided)
      vm.save {}

      assertEquals(AvatarShape.Cookie9Sided, db.person(marta.id)?.avatarShape)
      assertEquals(AvatarColor.Lavender, db.person(marta.id)?.avatarColor)
    }
}

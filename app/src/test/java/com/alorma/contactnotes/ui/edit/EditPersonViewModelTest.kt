package com.alorma.contactnotes.ui.edit

import android.content.ContextWrapper
import com.alorma.contactnotes.data.contacts.ResolvedContact
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.flags.ProFeatureFlagRunner
import com.alorma.contactnotes.data.pro.LocalProDataSource
import com.alorma.contactnotes.data.pro.ProRepository
import com.alorma.contactnotes.testing.FakeContactsGateway
import com.alorma.contactnotes.testing.FakeContactsPermission
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.FakeSettingsRepository
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.ui.person.PersonExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
  private val flags = LocalFeatureFlagRunner()
  private val pro = LocalProDataSource(initial = true)
  private val contactLink =
    ContactLinkFeature(ProFeatureFlagRunner(flags, ProRepository(pro), CoroutineScope(UnconfinedTestDispatcher())))
  private val permission = FakeContactsPermission(granted = false)
  private val contacts = FakeContactsGateway()
  private val settings = FakeSettingsRepository()

  private fun viewModel(personId: Long = 0L) =
    EditPersonViewModel(
      personId,
      db.repository,
      PersonExporter(ContextWrapper(null), db.repository),
      contactLink,
      permission,
      contacts,
      settings,
    )

  private fun enableContactLink() = flags.setOverride("contact_link_enabled", true)

  private val picked = ResolvedContact(id = 7, lookupKey = "lookup", name = "Marta Soler")

  @Test
  fun withTheFlagOffTheContactSectionIsHidden() {
    val vm = viewModel()

    assertEquals(ContactSection.Hidden, vm.state.value.contact.section)
  }

  @Test
  fun theContactSectionFollowsProWhileTheEditorIsOpen() {
    enableContactLink()
    pro.setPro(false)
    val vm = viewModel()
    assertEquals(ContactSection.Locked, vm.state.value.contact.section)

    pro.setPro(true)
    assertEquals(ContactSection.NotLinked, vm.state.value.contact.section)

    pro.setPro(false)
    assertEquals(ContactSection.Locked, vm.state.value.contact.section)

    flags.setOverride("contact_link_enabled", false)
    assertEquals(ContactSection.Hidden, vm.state.value.contact.section)
  }

  @Test
  fun withoutProALinkedPersonIsLockedAndSavingKeepsTheLink() =
    runTest {
      enableContactLink()
      pro.setPro(false)
      val person = db.addPerson("Marta Soler")
      db.repository.linkContact(person, picked.id, picked.lookupKey, picked.name)
      val vm = viewModel(person.id)
      assertEquals(ContactSection.Locked, vm.state.value.contact.section)

      vm.onAlias("Marta")
      var saved = false
      vm.save { saved = true }

      assertTrue(saved)
      assertEquals("Marta", db.repository.getPerson(person.id)?.alias)
      assertEquals(picked.id, db.repository.getPerson(person.id)?.contactId)
    }

  @Test
  fun theFirstLinkExplainsThenAsksForPermissionThenPicks() {
    enableContactLink()
    val vm = viewModel()
    assertEquals(ContactSection.NotLinked, vm.state.value.contact.section)
    assertEquals(ContactLinkStep.Explain, vm.state.value.contact.linkStep)

    vm.onContactsExplained()
    assertTrue(settings.state.value.contactsExplained)
    assertEquals(ContactLinkStep.RequestPermission, vm.state.value.contact.linkStep)

    permission.granted = true
    vm.onContactsPermissionChanged()
    assertEquals(ContactLinkStep.Pick, vm.state.value.contact.linkStep)
    assertEquals(ContactSection.NotLinked, vm.state.value.contact.section)
  }

  @Test
  fun theExplanationIsOnlyShownOnce() {
    enableContactLink()
    settings.state.value = settings.state.value.copy(contactsExplained = true)
    permission.granted = true

    val vm = viewModel()

    assertEquals(ContactLinkStep.Pick, vm.state.value.contact.linkStep)
  }

  @Test
  fun aDeniedPermissionShowsNoPermissionUntilItIsGranted() {
    enableContactLink()
    val vm = viewModel()
    vm.onContactsExplained()
    vm.onContactsPermissionChanged()
    assertEquals(ContactSection.NoPermission, vm.state.value.contact.section)

    permission.granted = true
    vm.onContactsPermissionChanged()
    assertEquals(ContactSection.NotLinked, vm.state.value.contact.section)
  }

  @Test
  fun linkingAnExistingPersonIsSavedAtOnce() {
    enableContactLink()
    val marta = db.addPerson("Marta")
    contacts.pickable["content://picked"] = picked
    val vm = viewModel(marta.id)

    vm.onContactPicked("content://picked")

    assertEquals(ContactSection.Linked("Marta Soler"), vm.state.value.contact.section)
    assertFalse(vm.state.value.canSave)
    val saved = db.person(marta.id)!!
    assertEquals(7L, saved.contactId)
    assertEquals("lookup", saved.contactLookupKey)
    assertEquals("Marta Soler", saved.contactName)
  }

  @Test
  fun unlinkingAnExistingPersonIsSavedAtOnceAndKeepsThePerson() =
    runTest {
      enableContactLink()
      val marta = db.addPerson("Marta", alias = "marta")
      db.repository.linkContact(marta, contactId = 7, lookupKey = "lookup", name = "Marta Soler")
      val vm = viewModel(marta.id)
      assertEquals(ContactSection.Linked("Marta Soler"), vm.state.value.contact.section)

      vm.onUnlinkContact()

      assertEquals(ContactSection.NotLinked, vm.state.value.contact.section)
      val saved = db.person(marta.id)!!
      assertNull(saved.contactId)
      assertNull(saved.contactLookupKey)
      assertNull(saved.contactName)
      assertEquals("marta", saved.alias)
    }

  @Test
  fun aNewPersonSavesItsLinkWithTheProfile() {
    enableContactLink()
    contacts.pickable["content://picked"] = picked
    val vm = viewModel()
    vm.onName("Marta")
    vm.onContactPicked("content://picked")

    var id = 0L
    vm.save { id = it }

    assertEquals(7L, db.person(id)!!.contactId)
    assertEquals("Marta Soler", db.person(id)!!.contactName)
  }

  @Test
  fun aContactLinkedToSomeoneElseIsNotLinked() =
    runTest {
      enableContactLink()
      val luna = db.addPerson("Luna", isProtected = true)
      db.repository.linkContact(luna, contactId = picked.id, lookupKey = picked.lookupKey, name = picked.name)
      val marta = db.addPerson("Marta")
      contacts.pickable["content://picked"] = picked
      val vm = viewModel(marta.id)

      vm.onContactPicked("content://picked")

      assertEquals(ContactSection.Taken, vm.state.value.contact.section)
      assertNull(db.person(marta.id)!!.contactId)

      // Un altre contacte sí que es vincula, i el missatge se'n va
      contacts.pickable["content://other"] = ResolvedContact(id = 8, lookupKey = "other", name = "Marta")
      vm.onContactPicked("content://other")

      assertEquals(ContactSection.Linked("Marta"), vm.state.value.contact.section)
    }

  @Test
  fun aNewPersonCannotTakeALinkedContact() =
    runTest {
      enableContactLink()
      val luna = db.addPerson("Luna")
      db.repository.linkContact(luna, contactId = 99, lookupKey = picked.lookupKey, name = picked.name)
      contacts.pickable["content://picked"] = picked
      val vm = viewModel()

      vm.onContactPicked("content://picked")

      assertEquals(ContactSection.Taken, vm.state.value.contact.section)
    }

  @Test
  fun relinkingTheSameContactIsAllowed() =
    runTest {
      enableContactLink()
      val marta = db.addPerson("Marta")
      db.repository.linkContact(marta, contactId = picked.id, lookupKey = picked.lookupKey, name = picked.name)
      contacts.pickable["content://picked"] = picked
      val vm = viewModel(marta.id)
      vm.onUnlinkContact()

      vm.onContactPicked("content://picked")

      assertEquals(ContactSection.Linked("Marta Soler"), vm.state.value.contact.section)
    }

  @Test
  fun aContactThatCannotBeReadIsNotLinked() {
    enableContactLink()
    val marta = db.addPerson("Marta")
    val vm = viewModel(marta.id)

    vm.onContactPicked("content://gone")

    assertEquals(ContactSection.NotLinked, vm.state.value.contact.section)
    assertNull(db.person(marta.id)!!.contactId)
  }

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
  fun savingAnExistingPersonKeepsTheContactLink() =
    runTest {
      val marta = db.addPerson("Marta")
      db.repository.linkContact(marta, contactId = 7, lookupKey = "lookup", name = "Marta Soler")
      val vm = viewModel(marta.id)
      vm.onName("Marta S")

      vm.save { }

      val saved = db.person(marta.id)!!
      assertEquals(7L, saved.contactId)
      assertEquals("lookup", saved.contactLookupKey)
      assertEquals("Marta Soler", saved.contactName)
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
}

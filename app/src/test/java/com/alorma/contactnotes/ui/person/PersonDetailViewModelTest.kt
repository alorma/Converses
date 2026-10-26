package com.alorma.contactnotes.ui.person

import android.content.ContextWrapper
import com.alorma.contactnotes.data.contacts.ResolvedContact
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.flags.ProFeatureFlagRunner
import com.alorma.contactnotes.data.pro.LocalProDataSource
import com.alorma.contactnotes.data.pro.ProRepository
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.testing.FakeContactsGateway
import com.alorma.contactnotes.testing.FakeContactsPermission
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()
  private val person = db.addPerson("Marta Soler")

  private fun at(
    day: Int,
    hour: Int,
  ): Long =
    LocalDate
      .of(2025, 3, day)
      .atTime(hour, 0)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()

  private val flags = LocalFeatureFlagRunner()
  private val pro = LocalProDataSource(initial = true)
  private val contactLink =
    ContactLinkFeature(ProFeatureFlagRunner(flags, ProRepository(pro), CoroutineScope(UnconfinedTestDispatcher())))
  private val permission = FakeContactsPermission(granted = true)
  private val contacts = FakeContactsGateway()
  private val contact = ResolvedContact(id = 7, lookupKey = "lookup", name = "Marta Soler Puig")

  private fun kotlinx.coroutines.test.TestScope.viewModel(): PersonDetailViewModel {
    val vm =
      PersonDetailViewModel(
        person.id,
        db.repository,
        PersonExporter(ContextWrapper(null), db.repository),
        contactLink,
        permission,
        contacts,
      )
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    return vm
  }

  private suspend fun linkToContact() {
    flags.setOverride("contact_link_enabled", true)
    db.repository.linkContact(person, contact.id, contact.lookupKey, contact.name)
  }

  @Test
  fun aLinkedContactIsResolvedWhenTheDetailOpens() =
    runTest {
      linkToContact()
      contacts.addContact(contact)
      val vm = viewModel()

      assertEquals(DetailContact.Linked(7, "lookup", "Marta Soler Puig"), vm.contact.value)
    }

  @Test
  fun theLinkedContactFollowsProWhileTheDetailIsOpen() =
    runTest {
      linkToContact()
      contacts.addContact(contact)
      pro.setPro(false)
      val vm = viewModel()
      assertEquals(DetailContact.None, vm.contact.value)

      pro.setPro(true)
      assertEquals(DetailContact.Linked(7, "lookup", "Marta Soler Puig"), vm.contact.value)

      // Sense Pro no es mostra, però el vincle es conserva
      pro.setPro(false)
      assertEquals(DetailContact.None, vm.contact.value)
      assertEquals(7L, db.repository.getPerson(person.id)?.contactId)
    }

  @Test
  fun aNewLookupKeyAndNameAreSavedWhenResolving() =
    runTest {
      linkToContact()
      // El lookup key antic encara resol el contacte, però ara en torna un altre i el nom ha canviat
      contacts.addContact(contact.copy(lookupKey = "lookup.agg", name = "Marta S."))
      contacts.contacts["lookup"] = contact.copy(lookupKey = "lookup.agg", name = "Marta S.")
      val vm = viewModel()

      assertEquals(DetailContact.Linked(7, "lookup.agg", "Marta S."), vm.contact.value)
      val saved = db.person(person.id)!!
      assertEquals("lookup.agg", saved.contactLookupKey)
      assertEquals("Marta S.", saved.contactName)
    }

  @Test
  fun aDeletedContactCanBeUnlinked() =
    runTest {
      linkToContact()
      val vm = viewModel()
      assertEquals(DetailContact.Deleted, vm.contact.value)

      vm.unlinkContact()

      assertEquals(DetailContact.None, vm.contact.value)
      assertNull(db.person(person.id)!!.contactLookupKey)
    }

  @Test
  fun withoutPermissionOrWithTheFlagOffThereIsNoContactRow() =
    runTest {
      linkToContact()
      contacts.addContact(contact)
      permission.granted = false
      assertEquals(DetailContact.None, viewModel().contact.value)

      permission.granted = true
      flags.setOverride("contact_link_enabled", false)
      assertEquals(DetailContact.None, viewModel().contact.value)
    }

  @Test
  fun withoutALinkThereIsNoContactRow() =
    runTest {
      flags.setOverride("contact_link_enabled", true)

      assertEquals(DetailContact.None, viewModel().contact.value)
    }

  @Test
  fun linkingFromTheEditorIsResolvedWhenComingBack() =
    runTest {
      flags.setOverride("contact_link_enabled", true)
      contacts.addContact(contact)
      val vm = viewModel()
      assertEquals(DetailContact.None, vm.contact.value)

      db.repository.linkContact(person, contact.id, contact.lookupKey, contact.name)

      assertEquals(DetailContact.Linked(7, "lookup", "Marta Soler Puig"), vm.contact.value)
    }

  @Test
  fun timelineIsNewestFirstAndCountsAreByKind() =
    runTest {
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
  fun channelsAreDerivedFromTheEntriesInTheFixedOrderWithoutRepeats() =
    runTest {
      db.addEntry(person.id, channel = ChannelType.SOCIAL, occurredAt = at(1, 10))
      db.addEntry(person.id, channel = ChannelType.CHAT, occurredAt = at(2, 10))
      db.addEntry(person.id, channel = ChannelType.IN_PERSON, occurredAt = at(3, 10))
      db.addEntry(person.id, channel = ChannelType.CHAT, occurredAt = at(4, 10))
      val vm = viewModel()

      assertEquals(listOf(ChannelType.IN_PERSON, ChannelType.CHAT, ChannelType.SOCIAL), vm.uiState.value.channels)
    }

  @Test
  fun channelsStayEmptyWithoutEntries() =
    runTest {
      val vm = viewModel()

      assertTrue(
        vm.uiState.value.channels
          .isEmpty(),
      )
    }

  @Test
  fun channelsFollowTheEntriesWhenOneIsAdded() =
    runTest {
      val vm = viewModel()
      assertTrue(
        vm.uiState.value.channels
          .isEmpty(),
      )

      vm.addEntry(EntryKind.SAID, ChannelType.PHONE, "call", occurredAt = null)

      assertEquals(listOf(ChannelType.PHONE), vm.uiState.value.channels)
    }

  @Test
  fun togglingAKindFiltersTheTimelineButNotTheCounts() =
    runTest {
      db.addEntry(person.id, EntryKind.SAID, text = "said")
      db.addEntry(person.id, EntryKind.HEARD, text = "heard")
      val vm = viewModel()

      vm.onKindToggled(EntryKind.SAID)
      assertEquals(
        listOf("heard"),
        vm.uiState.value.entries
          .map { it.text },
      )
      assertEquals(1, vm.uiState.value.saidCount)

      vm.onKindToggled(EntryKind.SAID)
      assertEquals(2, vm.uiState.value.entries.size)
    }

  @Test
  fun deselectingEveryKindLeavesTheTimelineEmpty() =
    runTest {
      db.addEntry(person.id, EntryKind.SAID)
      val vm = viewModel()

      EntryKind.entries.forEach { vm.onKindToggled(it) }

      assertTrue(
        vm.uiState.value.entries
          .isEmpty(),
      )
      assertTrue(
        vm.uiState.value.selectedKinds
          .isEmpty(),
      )
    }

  @Test
  fun addEntryUsesTheGivenTimeOrNow() =
    runTest {
      val vm = viewModel()
      val before = System.currentTimeMillis()

      vm.addEntry(EntryKind.HAPPENED, ChannelType.IN_PERSON, "met", occurredAt = null)
      vm.addEntry(EntryKind.SAID, ChannelType.PHONE, "called", occurredAt = 5_000)

      val entries =
        vm.uiState.value.entries
          .associateBy { it.text }
      assertTrue(entries.getValue("met").occurredAt >= before)
      assertEquals(5_000, entries.getValue("called").occurredAt)
    }

  @Test
  fun updateEntryKeepsTheDateWhenNoneIsGiven() =
    runTest {
      val entry = db.addEntry(person.id, occurredAt = 7_000)
      val vm = viewModel()

      vm.updateEntry(entry, EntryKind.HEARD, ChannelType.SOCIAL, "edited", occurredAt = null)

      val updated =
        vm.uiState.value.entries
          .single()
      assertEquals("edited", updated.text)
      assertEquals(EntryKind.HEARD, updated.kind)
      assertEquals(7_000, updated.occurredAt)
    }

  @Test
  fun deleteAndRestoreEntryRoundTrips() =
    runTest {
      val entry = db.addEntry(person.id, text = "keep me")
      val vm = viewModel()

      vm.deleteEntry(entry)
      assertTrue(
        vm.uiState.value.entries
          .isEmpty(),
      )

      vm.restoreEntry(entry)
      assertEquals(
        listOf("keep me"),
        vm.uiState.value.entries
          .map { it.text },
      )
    }

  @Test
  fun deleteDayOnlyRemovesThatDayAndChannel() =
    runTest {
      val target = db.addEntry(person.id, channel = ChannelType.CHAT, text = "a", occurredAt = at(5, 9))
      db.addEntry(person.id, channel = ChannelType.CHAT, text = "b", occurredAt = at(5, 21))
      db.addEntry(person.id, channel = ChannelType.SOCIAL, text = "other channel", occurredAt = at(5, 12))
      db.addEntry(person.id, channel = ChannelType.CHAT, text = "other day", occurredAt = at(6, 9))
      val vm = viewModel()

      assertEquals(2, vm.countDay(target))
      vm.deleteDay(target)

      assertEquals(
        setOf("other channel", "other day"),
        vm.uiState.value.entries
          .map { it.text }
          .toSet(),
      )
    }

  @Test
  fun moveEntryChangesItsOwner() =
    runTest {
      val other = db.addPerson("Jordi")
      val entry = db.addEntry(person.id)
      val vm = viewModel()

      vm.moveEntry(entry, other.id)

      assertTrue(
        vm.uiState.value.entries
          .isEmpty(),
      )
      assertEquals(1, db.repository.getEntries(other.id).size)
    }

  @Test
  fun archivingReportsBackWhenDone() =
    runTest {
      val vm = viewModel()
      var done = false

      vm.setArchived(true) { done = true }

      assertTrue(done)
      assertTrue(db.person(person.id)!!.archived)
    }
}

package com.alorma.contactnotes.data.contacts

import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.testing.FakeContactsGateway
import com.alorma.contactnotes.testing.FakeContactsPermission
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsReconcilerTest {
  private val db = FakeDb()
  private val flags = LocalFeatureFlagRunner(initial = mapOf("contact_link_enabled" to true))
  private val permission = FakeContactsPermission()
  private val gateway = FakeContactsGateway()
  private lateinit var reconciler: ContactsReconciler

  private fun TestScope.start() {
    reconciler =
      ContactsReconciler(
        repository = db.repository,
        contactLink = ContactLinkFeature(flags),
        permission = permission,
        gateway = gateway,
        scope = backgroundScope,
        debounceMillis = 500,
      )
    reconciler.start()
    settle()
  }

  private fun TestScope.settle() {
    runCurrent()
    advanceTimeBy(600)
    runCurrent()
  }

  private fun linkedPerson(
    name: String,
    contactId: Long,
    isProtected: Boolean = false,
    archived: Boolean = false,
  ): PersonEntity {
    gateway.addContact(ResolvedContact(id = contactId, lookupKey = "key$contactId", name = "$name contact"))
    val person = db.addPerson(name, isProtected = isProtected, archived = archived)
    val linked = person.copy(contactId = contactId, contactLookupKey = "key$contactId", contactName = "$name contact")
    update(linked)
    return linked
  }

  private fun update(person: PersonEntity) =
    db.people.update { list ->
      list.map {
        if (it.id ==
          person.id
        ) {
          person
        } else {
          it
        }
      }
    }

  private suspend fun rowPeople() = gateway.rows().map { it.personId }

  @Test
  fun linkedPeopleGetRowsAndTheNewLookupKeyIsSaved() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      val archived = linkedPerson("Bo", contactId = 11, archived = true)

      start()

      assertTrue(gateway.accountExists)
      assertEquals(listOf(ada.id, archived.id), rowPeople())
      assertEquals("key10.agg", db.person(ada.id)!!.contactLookupKey)
    }

  @Test
  fun reconcilingAgainChangesNothing() =
    runTest {
      linkedPerson("Ada", contactId = 10)
      start()
      val rows = gateway.rows()

      reconciler.refresh()
      settle()

      assertEquals(rows, gateway.rows())
    }

  @Test
  fun protectingRemovesTheRowsAndUnprotectingBringsThemBack() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      update(db.person(ada.id)!!.copy(isProtected = true))
      settle()
      assertEquals(emptyList<Long>(), rowPeople())

      update(db.person(ada.id)!!.copy(isProtected = false))
      settle()
      assertEquals(listOf(ada.id), rowPeople())
    }

  @Test
  fun deletingTheLastLinkedPersonRemovesRowsAndAccount() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      db.repository.deletePerson(db.person(ada.id)!!)
      settle()

      assertEquals(emptyList<Long>(), rowPeople())
      assertFalse(gateway.accountExists)
    }

  @Test
  fun aNewLanguageRedoesTheRows() =
    runTest {
      linkedPerson("Ada", contactId = 10)
      start()

      gateway.labels = listOf("Veure notes", "Crear nota")
      reconciler.refresh()
      settle()

      assertEquals(listOf("Veure notes", "Crear nota"), gateway.rows().single().labels)
    }

  @Test
  fun aDeletedAccountIsCreatedAgainOnRefresh() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      gateway.removeAccount()
      reconciler.refresh()
      settle()

      assertTrue(gateway.accountExists)
      assertEquals(listOf(ada.id), rowPeople())
    }

  @Test
  fun aDeletedContactLeavesNoRowsAndKeepsTheLink() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      gateway.contacts.clear()
      update(db.person(ada.id)!!.copy(contactId = 99))
      settle()

      assertEquals(emptyList<Long>(), rowPeople())
      assertEquals(99L, db.person(ada.id)!!.contactId)
    }

  @Test
  fun aContactDeletedWithTheSameIdLeavesNoNamelessContact() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      // El proveïdor conserva l'id: només les files soles diuen que el contacte ja no hi és
      gateway.deleteContact(10)
      reconciler.refresh()
      settle()

      assertEquals(emptyList<Long>(), rowPeople())
      assertEquals(10L, db.person(ada.id)!!.contactId)
    }

  @Test
  fun turningTheFlagOffLeavesNothingAndKeepsTheLinks() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      flags.setOverride("contact_link_enabled", false)
      settle()

      assertEquals(emptyList<Long>(), rowPeople())
      assertFalse(gateway.accountExists)
      assertEquals(10L, db.person(ada.id)!!.contactId)
    }

  @Test
  fun withoutPermissionOnlyTheAccountIsRemoved() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      permission.granted = false
      gateway.denied = true
      reconciler.refresh()
      settle()

      assertFalse(gateway.accountExists)
      assertEquals("key10.agg", db.person(ada.id)!!.contactLookupKey)

      permission.granted = true
      gateway.denied = false
      reconciler.refresh()
      settle()

      assertTrue(gateway.accountExists)
      assertEquals(listOf(ada.id), rowPeople())
    }

  @Test
  fun aLinkChangedMeanwhileIsNotOverwritten() =
    runTest {
      val ada = linkedPerson("Ada", contactId = 10)
      start()

      db.repository.unlinkContact(db.person(ada.id)!!)
      db.repository.refreshContact(ada.id, "key10.agg", ResolvedContact(id = 10, lookupKey = "stale", name = "Ada"))

      assertEquals(null, db.person(ada.id)!!.contactLookupKey)
    }
}

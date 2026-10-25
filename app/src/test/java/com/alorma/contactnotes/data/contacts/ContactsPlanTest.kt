package com.alorma.contactnotes.data.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsPlanTest {
  private val labels = listOf("View notes", "Create note")
  private val ada = LinkedPerson(id = 1, contactId = 10, lookupKey = "ada", isProtected = false)
  private val luna = LinkedPerson(id = 2, contactId = 20, lookupKey = "luna", isProtected = true)

  private fun rowsOf(
    person: LinkedPerson,
    contactId: Long = person.contactId,
    labels: List<String> = this.labels,
  ) = ContactRows(rawContactId = person.id * 100, personId = person.id, contactId = contactId, labels = labels)

  private fun plan(
    people: List<LinkedPerson> = listOf(ada),
    enabled: Boolean = true,
    granted: Boolean = true,
    hasAccount: Boolean = true,
    rows: List<ContactRows> = emptyList(),
    labels: List<String> = this.labels,
  ) = ContactsPlan.of(people, enabled, granted, hasAccount, rows, labels)

  @Test
  fun linkedPersonWithoutRowsGetsThem() {
    assertEquals(ContactsPlan(create = listOf(ada)), plan())
  }

  @Test
  fun rowsThatAlreadyMatchNeedNothing() {
    assertTrue(plan(rows = listOf(rowsOf(ada))).isEmpty)
  }

  @Test
  fun firstLinkCreatesTheAccount() {
    assertEquals(ContactsPlan(ensureAccount = true, create = listOf(ada)), plan(hasAccount = false))
  }

  @Test
  fun protectedPersonNeverHasRows() {
    assertTrue(plan(people = listOf(luna)).isEmpty)
  }

  @Test
  fun protectingPersonWithRowsRemovesThem() {
    assertEquals(ContactsPlan(remove = listOf(luna.id)), plan(people = listOf(luna), rows = listOf(rowsOf(luna))))
  }

  @Test
  fun accountStaysWhileOnlyProtectedPeopleAreLinked() {
    assertTrue(plan(people = listOf(luna)).isEmpty)
    assertEquals(ContactsPlan(ensureAccount = true), plan(people = listOf(luna), hasAccount = false))
  }

  @Test
  fun archivedPersonKeepsTheRows() {
    // El pla no sap res d'arxivar: una arxivada és una persona vinculada més
    assertTrue(plan(rows = listOf(rowsOf(ada))).isEmpty)
  }

  @Test
  fun deletedOrUnlinkedPersonLosesRowsAndLastOneTheAccount() {
    assertEquals(
      ContactsPlan(removeAccount = true, remove = listOf(ada.id)),
      plan(people = emptyList(), rows = listOf(rowsOf(ada))),
    )
  }

  @Test
  fun rowsGluedToAnotherContactAreRedone() {
    assertEquals(ContactsPlan(create = listOf(ada)), plan(rows = listOf(rowsOf(ada, contactId = 99))))
  }

  @Test
  fun rowsInAnotherLanguageAreRedone() {
    assertEquals(
      ContactsPlan(create = listOf(ada)),
      plan(rows = listOf(rowsOf(ada, labels = listOf("Veure notes", "Crear nota")))),
    )
  }

  @Test
  fun twoPeopleOnTheSameContactOnlyTheFirstLinkedHasRows() {
    val bob = LinkedPerson(id = 3, contactId = ada.contactId, lookupKey = "bob", isProtected = false)
    assertEquals(ContactsPlan(create = listOf(ada)), plan(people = listOf(bob, ada)))
    // Dos contactes amb files que es fusionen: les de la segona persona se'n van
    assertEquals(
      ContactsPlan(remove = listOf(bob.id)),
      plan(people = listOf(ada, bob), rows = listOf(rowsOf(ada), rowsOf(bob))),
    )
  }

  @Test
  fun aProtectedPersonDoesNotTakeTheContactFromAnother() {
    val bob = LinkedPerson(id = 3, contactId = luna.contactId, lookupKey = "bob", isProtected = false)
    assertEquals(ContactsPlan(create = listOf(bob)), plan(people = listOf(luna, bob)))
  }

  @Test
  fun rowsLeftAloneByADeletedContactAreRedone() {
    assertEquals(ContactsPlan(create = listOf(ada)), plan(rows = listOf(rowsOf(ada).copy(alone = true))))
  }

  @Test
  fun missingOrDuplicatedRowsAreRedone() {
    assertEquals(ContactsPlan(create = listOf(ada)), plan(rows = listOf(rowsOf(ada, labels = labels.take(1)))))
    assertEquals(ContactsPlan(create = listOf(ada)), plan(rows = listOf(rowsOf(ada), rowsOf(ada))))
  }

  @Test
  fun deletedAccountIsCreatedAgainAndRowsRedone() {
    assertEquals(
      ContactsPlan(ensureAccount = true, create = listOf(ada)),
      plan(hasAccount = false, rows = listOf(rowsOf(ada))),
    )
  }

  @Test
  fun flagOffLeavesNothingNotEvenTheAccount() {
    assertEquals(
      ContactsPlan(removeAccount = true, remove = listOf(ada.id)),
      plan(enabled = false, rows = listOf(rowsOf(ada))),
    )
    assertTrue(plan(enabled = false, hasAccount = false).isEmpty)
  }

  @Test
  fun withoutPermissionOnlyTheAccountGoesAndLinksStay() {
    assertEquals(ContactsPlan(removeAccount = true), plan(granted = false))
    assertTrue(plan(granted = false, hasAccount = false).isEmpty)
  }
}

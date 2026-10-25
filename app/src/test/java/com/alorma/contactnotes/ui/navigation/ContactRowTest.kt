package com.alorma.contactnotes.ui.navigation

import com.alorma.contactnotes.data.db.PersonEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactRowTest {
  private val marta = PersonEntity(id = 2, name = "Marta", createdAt = 0, updatedAt = 0)
  private val luna = PersonEntity(id = 1, name = "Luna", isProtected = true, createdAt = 0, updatedAt = 0)

  @Test
  fun viewNotesOpensThePerson() {
    assertEquals(PersonRoute(2), contactRowRoute(ContactRowAction.ViewNotes, marta, enabled = true))
  }

  @Test
  fun createNoteOpensThePersonWithTheSheet() {
    assertEquals(
      PersonRoute(2, openNewEntry = true),
      contactRowRoute(ContactRowAction.CreateNote, marta, enabled = true),
    )
  }

  @Test
  fun aProtectedPersonGoesThroughTheLockFirst() {
    assertEquals(PersonLockedRoute(1), contactRowRoute(ContactRowAction.ViewNotes, luna, enabled = true))
    assertEquals(
      PersonLockedRoute(1, openNewEntry = true),
      contactRowRoute(ContactRowAction.CreateNote, luna, enabled = true),
    )
  }

  @Test
  fun anArchivedPersonStillOpens() {
    assertEquals(PersonRoute(2), contactRowRoute(ContactRowAction.ViewNotes, marta.copy(archived = true), true))
  }

  @Test
  fun theNormalAppOpensWhenTheFlagIsOffThePersonIsGoneOrTheRowIsNotOurs() {
    assertNull(contactRowRoute(ContactRowAction.ViewNotes, marta, enabled = false))
    assertNull(contactRowRoute(ContactRowAction.ViewNotes, person = null, enabled = true))
    assertNull(contactRowRoute(action = null, person = marta, enabled = true))
  }

  @Test
  fun mimeTypesMatchTheirActionAndCarryTheApplicationId() {
    ContactRowAction.entries.forEach { action ->
      assertEquals(action, contactRowAction(RELEASE, contactRowMimeType(RELEASE, action)))
    }
    assertNotEquals(
      contactRowMimeType(RELEASE, ContactRowAction.ViewNotes),
      contactRowMimeType(DEBUG, ContactRowAction.ViewNotes),
    )
    assertNull(contactRowAction(RELEASE, contactRowMimeType(DEBUG, ContactRowAction.ViewNotes)))
    assertNull(contactRowAction(RELEASE, "vnd.android.cursor.item/phone_v2"))
    assertNull(contactRowAction(RELEASE, null))
  }

  private companion object {
    const val RELEASE = "com.alorma.contactnotes"
    const val DEBUG = "com.alorma.contactnotes.dev"
  }
}

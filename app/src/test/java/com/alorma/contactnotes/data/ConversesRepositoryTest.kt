package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversesRepositoryTest {
  private val db = FakeDb()
  private val repository = db.repository

  @Test
  fun protectedPeopleDoNotExposeTheirLastEntryNorChannels() =
    runTest {
      val open = db.addPerson("Marta")
      val secret = db.addPerson("Luna", isProtected = true)
      db.addEntry(open.id, channel = ChannelType.CHAT, text = "hola")
      db.addEntry(secret.id, channel = ChannelType.SOCIAL, text = "secret")

      val list = repository.observePeople().first().associateBy { it.person.id }

      assertEquals("hola", list.getValue(open.id).lastEntry?.text)
      assertEquals(1, list.getValue(open.id).channels.size)
      assertNull(list.getValue(secret.id).lastEntry)
      assertTrue(list.getValue(secret.id).channels.isEmpty())
    }

  @Test
  fun listIsOrderedByLastActivityAndFallsBackToCreationDate() =
    runTest {
      val old = db.addPerson("Old", createdAt = 1_000)
      val recent = db.addPerson("Recent", createdAt = 2_000)
      db.addEntry(old.id, occurredAt = 5_000)

      assertEquals(listOf(old.id, recent.id), repository.observePeople().first().map { it.person.id })
    }

  @Test
  fun archivedAndActiveListsAreSeparate() =
    runTest {
      val active = db.addPerson("Active")
      val archived = db.addPerson("Archived", archived = true)

      assertEquals(listOf(active.id), repository.observePeople(archived = false).first().map { it.person.id })
      assertEquals(listOf(archived.id), repository.observePeople(archived = true).first().map { it.person.id })
    }

  @Test
  fun aliasConflictIgnoresCaseTheOwnPersonAndArchivedPeople() =
    runTest {
      val marta = db.addPerson("Marta", alias = "Marta")
      db.addPerson("Gone", alias = "gone", archived = true)

      assertTrue(repository.isAliasTaken("marta"))
      assertFalse(repository.isAliasTaken("marta", excludePersonId = marta.id))
      assertFalse(repository.isAliasTaken("gone"))
    }

  @Test
  fun savePersonCreatesAndThenReplacesFacts() =
    runTest {
      val draft = db.addPerson("Draft").copy(id = 0)
      db.people.value = emptyList()
      val id =
        repository.savePerson(
          draft,
          listOf(
            com.alorma.contactnotes.data.db
              .PinnedFactEntity(personId = 0, label = "a", value = "b", position = 0),
          ),
        )
      assertEquals(1, repository.observePinnedFacts(id).first().size)

      repository.savePerson(db.person(id)!!, emptyList())
      assertTrue(repository.observePinnedFacts(id).first().isEmpty())
    }

  @Test
  fun setProtectedAndArchivedUpdateThePerson() =
    runTest {
      val person = db.addPerson("Marta")

      repository.setProtected(person, true)
      repository.setArchived(db.person(person.id)!!, true)

      val stored = db.person(person.id)!!
      assertTrue(stored.isProtected)
      assertTrue(stored.archived)
    }

  @Test
  fun deletingAPersonRemovesItsEntries() =
    runTest {
      val person = db.addPerson("Marta")
      db.addEntry(person.id, EntryKind.HEARD)

      repository.deletePerson(person)

      assertTrue(repository.getEntries(person.id).isEmpty())
    }
}

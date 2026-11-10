package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La identitat de sincronització (`docs/sync`): tota escriptura del repositori deixa la fila pendent de pujar, amb la
 * data de modificació, encara que la fila vingués neta.
 */
class SyncIdentityTest {
  private val db = FakeDb().apply { clock = { NOW } }
  private val repository = db.repository

  private fun entry(id: Long) = db.entries.value.single { it.id == id }

  private fun task(id: Long) = db.tasks.value.single { it.id == id }

  private fun channel(id: Long) = db.channels.value.single { it.id == id }

  private fun person(id: Long) = db.people.value.single { it.id == id }

  private fun nothingIsDirty() =
    db.people.value.none { it.dirty } &&
      db.entries.value.none { it.dirty } &&
      db.tasks.value.none { it.dirty } &&
      db.channels.value.none { it.dirty }

  @Test
  fun addEntryStampsIt() =
    runTest {
      val anna = db.addPerson("Anna")

      val id =
        repository.addEntry(
          EntryEntity(
            personId = anna.id,
            kind = EntryKind.SAID,
            channelType = ChannelType.CHAT,
            text = "hola",
            occurredAt = 10,
            dirty = false,
          ),
        )

      assertTrue(entry(id).dirty)
      assertEquals(NOW, entry(id).modifiedAt)
    }

  @Test
  fun updateEntryStampsACleanRow() =
    runTest {
      val anna = db.addPerson("Anna")
      db.addEntry(anna.id)
      db.markAllClean()
      val clean = db.entries.value.single()

      repository.updateEntry(clean.copy(text = "x"))

      assertTrue(entry(clean.id).dirty)
      assertEquals(NOW, entry(clean.id).modifiedAt)
      assertEquals(clean.syncId, entry(clean.id).syncId)
      assertEquals("x", entry(clean.id).text)
    }

  @Test
  fun moveEntryStampsIt() =
    runTest {
      val anna = db.addPerson("Anna")
      val other = db.addPerson("Bernat")
      db.addEntry(anna.id)
      db.markAllClean()
      val clean = db.entries.value.single()

      repository.moveEntry(clean, other.id)

      assertTrue(entry(clean.id).dirty)
      assertEquals(NOW, entry(clean.id).modifiedAt)
      assertEquals(other.id, entry(clean.id).personId)
    }

  @Test
  fun savePersonStampsANewOne() =
    runTest {
      val id =
        repository.savePerson(PersonEntity(name = "Anna", createdAt = 1, updatedAt = 1, dirty = false), emptyList())

      assertTrue(person(id).dirty)
      assertEquals(NOW, person(id).updatedAt)
    }

  @Test
  fun savePersonWithOnlyFactsChangedStampsThePerson() =
    runTest {
      db.addPerson("Anna")
      db.markAllClean()
      val clean = db.people.value.single()

      val fact = PinnedFactEntity(personId = clean.id, label = "L", value = "V", position = 0)

      repository.savePerson(clean, listOf(fact))

      assertTrue(person(clean.id).dirty)
      assertEquals(NOW, person(clean.id).updatedAt)
      assertEquals(clean.syncId, person(clean.id).syncId)
    }

  @Test
  fun setArchivedAndSetProtectedStampThePerson() =
    runTest {
      db.addPerson("Anna")
      db.addPerson("Bernat")
      db.markAllClean()
      val (anna, bernat) = db.people.value

      repository.setArchived(anna, true)
      repository.setProtected(bernat, true)

      assertTrue(person(anna.id).dirty)
      assertEquals(NOW, person(anna.id).updatedAt)
      assertTrue(person(bernat.id).dirty)
      assertEquals(NOW, person(bernat.id).updatedAt)
    }

  @Test
  fun saveTaskStampsNewAndEdited() =
    runTest {
      val anna = db.addPerson("Anna")
      db.addTask(anna.id)
      db.markAllClean()
      val clean = db.tasks.value.single()

      val newId = repository.saveTask(TaskEntity(personId = anna.id, text = "nova", createdAt = 1, dirty = false))
      repository.saveTask(clean.copy(text = "editada"))

      assertTrue(task(newId).dirty)
      assertEquals(NOW, task(newId).modifiedAt)
      assertTrue(task(clean.id).dirty)
      assertEquals(NOW, task(clean.id).modifiedAt)
    }

  @Test
  fun markTaskDoneAndReopenStampIt() =
    runTest {
      val anna = db.addPerson("Anna")
      db.addTask(anna.id)
      db.markAllClean()
      val clean = db.tasks.value.single()

      repository.markTaskDone(clean, at = 7_000)

      assertTrue(task(clean.id).dirty)
      assertEquals(7_000L, task(clean.id).modifiedAt)
      assertEquals(7_000L, task(clean.id).doneAt)

      db.markAllClean()
      repository.reopenTask(clean)

      assertTrue(task(clean.id).dirty)
      assertEquals(NOW, task(clean.id).modifiedAt)
      assertEquals(null, task(clean.id).doneAt)
    }

  @Test
  fun createAndRenameCustomChannelStampIt() =
    runTest {
      val created = repository.createCustomChannel(ChannelType.CHAT, "A", now = 7_000) as CustomChannelResult.Saved

      assertTrue(channel(created.id).dirty)
      assertEquals(7_000L, channel(created.id).modifiedAt)

      db.markAllClean()
      repository.renameCustomChannel(channel(created.id), "B")

      assertTrue(channel(created.id).dirty)
      assertEquals(NOW, channel(created.id).modifiedAt)
    }

  @Test
  fun changingAChannelGroupStampsItsEntries() =
    runTest {
      val anna = db.addPerson("Anna")
      val whatsapp = db.addCustomChannel("WhatsApp")
      val first = db.addEntry(anna.id, customChannelId = whatsapp.id)
      val second = db.addEntry(anna.id, customChannelId = whatsapp.id)
      val outside = db.addEntry(anna.id)
      db.markAllClean()

      repository.changeCustomChannelGroup(channel(whatsapp.id), ChannelType.SOCIAL)

      assertTrue(channel(whatsapp.id).dirty)
      assertEquals(NOW, channel(whatsapp.id).modifiedAt)
      listOf(first, second).forEach {
        assertTrue(entry(it.id).dirty)
        assertEquals(NOW, entry(it.id).modifiedAt)
        assertEquals(ChannelType.SOCIAL, entry(it.id).channelType)
      }
      assertFalse(entry(outside.id).dirty)
    }

  @Test
  fun aRejectedChannelNameStampsNothing() =
    runTest {
      db.addCustomChannel("WhatsApp")
      val signal = db.addCustomChannel("Signal")
      db.markAllClean()

      val result = repository.renameCustomChannel(channel(signal.id), "whatsapp")

      assertEquals(CustomChannelResult.NameTaken, result)
      assertTrue(nothingIsDirty())
    }

  @Test
  fun readsDoNotStamp() =
    runTest {
      val anna = db.addPerson("Anna")
      db.addEntry(anna.id)
      db.addTask(anna.id)
      db.markAllClean()

      repository.getPerson(anna.id)
      repository.getEntries(anna.id)
      repository.observePeople().first()

      assertTrue(nothingIsDirty())
    }

  private companion object {
    const val NOW = 5_000L
  }
}

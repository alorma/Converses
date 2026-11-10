package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.db.SyncTombstoneEntity
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.testing.FakeDb
import java.lang.reflect.Modifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

  // Esborrats: cada fila que desapareix deixa un tombstone, també les que cauen en cascada.

  private fun tombstoned() =
    db.tombstones.value
      .map { it.syncId }
      .toSet()

  @Test
  fun deleteEntryLeavesATombstone() =
    runTest {
      val anna = db.addPerson("Anna")
      val entry = db.addEntry(anna.id)

      repository.deleteEntry(entry)

      assertEquals(listOf(SyncTombstoneEntity(entry.syncId, deletedAt = NOW, dirty = true)), db.tombstones.value)
      assertTrue(db.entries.value.isEmpty())
    }

  @Test
  fun undoingAnEntryDeleteRemovesItsTombstone() =
    runTest {
      val anna = db.addPerson("Anna")
      val entry = db.addEntry(anna.id)
      db.markAllClean()

      repository.deleteEntry(entry)
      repository.addEntry(entry)

      assertTrue(db.tombstones.value.isEmpty())
      val back = db.entries.value.single()
      assertEquals(entry.syncId, back.syncId)
      assertTrue(back.dirty)
      assertEquals(NOW, back.modifiedAt)
    }

  @Test
  fun deletePersonLeavesTombstonesForItsEntriesAndTasks() =
    runTest {
      val anna = db.addPerson("Anna")
      val first = db.addEntry(anna.id)
      val second = db.addEntry(anna.id)
      val task = db.addTask(anna.id)
      repository.savePerson(anna, listOf(PinnedFactEntity(personId = anna.id, label = "L", value = "V", position = 0)))
      val other = db.addPerson("Bernat")
      db.addEntry(other.id)

      repository.deletePerson(anna)

      assertEquals(setOf(anna.syncId, first.syncId, second.syncId, task.syncId), tombstoned())
      assertTrue(db.tombstones.value.all { it.deletedAt == NOW && it.dirty })
      assertEquals(listOf(other.id), db.people.value.map { it.id })
      assertEquals(1, db.entries.value.size)
    }

  @Test
  fun deleteEntriesInRangeLeavesATombstonePerEntry() =
    runTest {
      val anna = db.addPerson("Anna")
      val first = db.addEntry(anna.id, occurredAt = 10)
      val second = db.addEntry(anna.id, occurredAt = 20)
      db.addEntry(anna.id, occurredAt = 30)

      val deleted = repository.deleteEntriesInRange(anna.id, ChannelType.CHAT, from = 10, to = 30)

      assertEquals(2, deleted)
      assertEquals(setOf(first.syncId, second.syncId), tombstoned())
    }

  @Test
  fun deleteTaskLeavesATombstone() =
    runTest {
      val anna = db.addPerson("Anna")
      val task = db.addTask(anna.id)

      repository.deleteTask(task)

      assertEquals(listOf(SyncTombstoneEntity(task.syncId, deletedAt = NOW, dirty = true)), db.tombstones.value)
    }

  @Test
  fun deletingAChannelStampsItsEntriesAndLeavesATombstone() =
    runTest {
      val anna = db.addPerson("Anna")
      val whatsapp = db.addCustomChannel("WhatsApp")
      val first = db.addEntry(anna.id, customChannelId = whatsapp.id)
      val second = db.addEntry(anna.id, customChannelId = whatsapp.id)
      val outside = db.addEntry(anna.id)
      db.markAllClean()

      repository.deleteCustomChannel(whatsapp)

      assertEquals(setOf(whatsapp.syncId), tombstoned())
      listOf(first, second).forEach {
        assertTrue(entry(it.id).dirty)
        assertEquals(NOW, entry(it.id).modifiedAt)
        assertEquals(null, entry(it.id).customChannelId)
      }
      assertFalse(entry(outside.id).dirty)
    }

  @Test
  fun deletingTheSameSyncIdTwiceKeepsOneTombstone() =
    runTest {
      val anna = db.addPerson("Anna")
      val entry = db.addEntry(anna.id)

      repository.deleteEntry(entry)
      repository.addEntry(entry)
      repository.deleteEntry(db.entries.value.single())

      assertEquals(listOf(entry.syncId), db.tombstones.value.map { it.syncId })
    }

  // La identitat no canvia: el `syncId` és el de la fila desada, porti el que porti l'entitat que arriba.

  @Test
  fun savingAPersonBuiltFromScratchKeepsItsSyncId() =
    runTest {
      val anna = db.addPerson("Anna")

      repository.savePerson(PersonEntity(id = anna.id, name = "Anna B", createdAt = 1, updatedAt = 1), emptyList())

      assertEquals("Anna B", person(anna.id).name)
      assertEquals(anna.syncId, person(anna.id).syncId)
    }

  @Test
  fun updatingRowsBuiltFromScratchKeepsTheirSyncId() =
    runTest {
      val anna = db.addPerson("Anna")
      val entry = db.addEntry(anna.id)
      val task = db.addTask(anna.id)
      val channel = db.addCustomChannel("WhatsApp")

      repository.updateEntry(entry.copy(text = "x", syncId = "other"))
      repository.moveEntry(entry.copy(syncId = "other"), anna.id)
      repository.saveTask(task.copy(text = "x", syncId = "other"))
      repository.renameCustomChannel(channel.copy(syncId = "other"), "Signal")
      repository.changeCustomChannelGroup(channel.copy(name = "Signal", syncId = "other"), ChannelType.SOCIAL)
      repository.setArchived(anna.copy(syncId = "other"), true)
      repository.setProtected(anna.copy(syncId = "other"), true)

      assertEquals(entry.syncId, entry(entry.id).syncId)
      assertEquals(task.syncId, task(task.id).syncId)
      assertEquals(channel.syncId, channel(channel.id).syncId)
      assertEquals(anna.syncId, person(anna.id).syncId)
    }

  @Test
  fun deletingWithAStaleEntityTombstonesTheStoredSyncId() =
    runTest {
      val anna = db.addPerson("Anna")
      val entry = db.addEntry(anna.id)
      val task = db.addTask(anna.id)
      val channel = db.addCustomChannel("WhatsApp")

      repository.deleteEntry(entry.copy(syncId = "other"))
      repository.deleteTask(task.copy(syncId = "other"))
      repository.deleteCustomChannel(channel.copy(syncId = "other"))
      repository.deletePerson(anna.copy(syncId = "other"))

      assertEquals(setOf(anna.syncId, entry.syncId, task.syncId, channel.syncId), tombstoned())
    }

  @Test
  fun theFakeRejectsADuplicateSyncIdLikeTheUniqueIndex() =
    runTest {
      val anna = db.addPerson("Anna")
      val entry = db.addEntry(anna.id)

      assertThrows(IllegalStateException::class.java) {
        runBlocking { repository.addEntry(entry.copy(id = 0)) }
      }
    }

  /** Si afegeixes un mètode d'escriptura al repositori: segella'l, afegeix-ne el test i apunta'l aquí. */
  private val writes =
    setOf(
      "savePerson",
      "deletePerson",
      "setArchived",
      "setProtected",
      "addEntry",
      "updateEntry",
      "deleteEntry",
      "moveEntry",
      "deleteEntriesInRange",
      "saveTask",
      "deleteTask",
      "markTaskDone",
      "reopenTask",
      "createCustomChannel",
      "renameCustomChannel",
      "changeCustomChannelGroup",
      "saveCustomChannel",
      "deleteCustomChannel",
    )

  @Test
  fun everyWriteMethodOfTheRepositoryIsCovered() {
    val reads = Regex("^(observe|get|is|count).*")
    val public =
      ConversesRepository::class.java.declaredMethods
        .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic && '$' !in it.name }
        .map { it.name }
        .toSet()

    assertEquals(writes, public.filterNot(reads::matches).toSet())
  }

  private companion object {
    const val NOW = 5_000L
  }
}

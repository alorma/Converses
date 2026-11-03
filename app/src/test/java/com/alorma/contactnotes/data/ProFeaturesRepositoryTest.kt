package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Canals propis i pendents (Pro) al repositori. */
class ProFeaturesRepositoryTest {
  private val db = FakeDb()
  private val repository = db.repository

  @Test
  fun createTrimsTheNameAndSavesTheChannelInItsGroup() =
    runTest {
      val result = repository.createCustomChannel(ChannelType.CHAT, "  WhatsApp  ", now = 5)

      val id = (result as CustomChannelResult.Saved).id
      val channel = repository.getCustomChannel(id)!!
      assertEquals("WhatsApp", channel.name)
      assertEquals(ChannelType.CHAT, channel.channelType)
      assertEquals(5L, channel.createdAt)
    }

  @Test
  fun aBlankNameIsRejected() =
    runTest {
      assertEquals(CustomChannelResult.NameBlank, repository.createCustomChannel(ChannelType.CHAT, "   "))
      assertTrue(db.channels.value.isEmpty())
    }

  @Test
  fun theNameIsUniqueWithinTheGroupIgnoringCase() =
    runTest {
      db.addCustomChannel("WhatsApp", ChannelType.CHAT)

      assertEquals(CustomChannelResult.NameTaken, repository.createCustomChannel(ChannelType.CHAT, "whatsapp "))
      assertTrue(repository.createCustomChannel(ChannelType.SOCIAL, "WhatsApp") is CustomChannelResult.Saved)
      assertTrue(repository.createCustomChannel(ChannelType.CHAT, "WhatsApp feina") is CustomChannelResult.Saved)
    }

  @Test
  fun renamingKeepsTheEntriesAndCanChangeTheCaseOfItsOwnName() =
    runTest {
      val channel = db.addCustomChannel("whatsapp")
      db.addCustomChannel("Telegram")
      val person = db.addPerson("Marta")
      db.addEntry(person.id, customChannelId = channel.id)

      assertEquals(CustomChannelResult.Saved(channel.id), repository.renameCustomChannel(channel, "WhatsApp"))
      assertEquals(CustomChannelResult.NameTaken, repository.renameCustomChannel(channel, "telegram"))

      assertEquals("WhatsApp", repository.getCustomChannel(channel.id)!!.name)
      assertEquals(
        channel.id,
        db.entries.value
          .single()
          .customChannelId,
      )
    }

  @Test
  fun changingTheGroupMovesItsEntriesButNotTheOthers() =
    runTest {
      val channel = db.addCustomChannel("Empresa", ChannelType.CHAT)
      val person = db.addPerson("Marta")
      val moved = db.addEntry(person.id, channel = ChannelType.CHAT, customChannelId = channel.id)
      val untouched = db.addEntry(person.id, channel = ChannelType.CHAT)

      val result = repository.changeCustomChannelGroup(channel, ChannelType.OTHER)

      assertEquals(CustomChannelResult.Saved(channel.id), result)
      assertEquals(ChannelType.OTHER, repository.getCustomChannel(channel.id)!!.channelType)
      val byId = db.entries.value.associateBy { it.id }
      assertEquals(ChannelType.OTHER, byId.getValue(moved.id).channelType)
      assertEquals(ChannelType.CHAT, byId.getValue(untouched.id).channelType)
    }

  @Test
  fun changingToAGroupWithTheSameNameIsRejectedAndMovesNothing() =
    runTest {
      val channel = db.addCustomChannel("Feina", ChannelType.CHAT)
      db.addCustomChannel("feina", ChannelType.OTHER)
      val person = db.addPerson("Marta")
      db.addEntry(person.id, channel = ChannelType.CHAT, customChannelId = channel.id)

      assertEquals(CustomChannelResult.NameTaken, repository.changeCustomChannelGroup(channel, ChannelType.OTHER))
      assertEquals(ChannelType.CHAT, repository.getCustomChannel(channel.id)!!.channelType)
      assertEquals(
        ChannelType.CHAT,
        db.entries.value
          .single()
          .channelType,
      )
    }

  @Test
  fun editingRenamesAndChangesTheGroupAtOnceMovingItsEntries() =
    runTest {
      val channel = db.addCustomChannel("Feina", ChannelType.CHAT)
      val person = db.addPerson("Marta")
      db.addEntry(person.id, channel = ChannelType.CHAT, customChannelId = channel.id)

      val result = repository.saveCustomChannel(channel, ChannelType.OTHER, " Empresa ")

      assertEquals(CustomChannelResult.Saved(channel.id), result)
      val saved = repository.getCustomChannel(channel.id)!!
      assertEquals("Empresa", saved.name)
      assertEquals(ChannelType.OTHER, saved.channelType)
      assertEquals(
        ChannelType.OTHER,
        db.entries.value
          .single()
          .channelType,
      )
    }

  @Test
  fun editingIntoATakenNameOfTheNewGroupChangesNothing() =
    runTest {
      val channel = db.addCustomChannel("Feina", ChannelType.CHAT)
      db.addCustomChannel("empresa", ChannelType.OTHER)

      assertEquals(
        CustomChannelResult.NameTaken,
        repository.saveCustomChannel(channel, ChannelType.OTHER, "Empresa"),
      )
      assertEquals(channel, repository.getCustomChannel(channel.id))
    }

  @Test
  fun savingWithoutAChannelCreatesOne() =
    runTest {
      val result = repository.saveCustomChannel(null, ChannelType.SOCIAL, "Bluesky")

      val id = (result as CustomChannelResult.Saved).id
      assertEquals(ChannelType.SOCIAL, repository.getCustomChannel(id)!!.channelType)
    }

  @Test
  fun deletingAChannelLeavesItsEntriesInTheGroup() =
    runTest {
      val channel = db.addCustomChannel("WhatsApp", ChannelType.CHAT)
      val person = db.addPerson("Marta")
      db.addEntry(person.id, channel = ChannelType.CHAT, text = "hola", customChannelId = channel.id)

      repository.deleteCustomChannel(channel)

      val entry = db.entries.value.single()
      assertNull(entry.customChannelId)
      assertEquals(ChannelType.CHAT, entry.channelType)
      assertEquals("hola", entry.text)
      assertNull(repository.getCustomChannel(channel.id))
    }

  @Test
  fun channelsAreListedInGroupOrderThenByNameWithTheirEntryCount() =
    runTest {
      val other = db.addCustomChannel("Empresa", ChannelType.OTHER)
      val telegram = db.addCustomChannel("telegram", ChannelType.CHAT)
      val whatsapp = db.addCustomChannel("WhatsApp", ChannelType.CHAT)
      val inPerson = db.addCustomChannel("Gimnàs", ChannelType.IN_PERSON)
      val person = db.addPerson("Marta")
      db.addEntry(person.id, customChannelId = whatsapp.id)
      db.addEntry(person.id, customChannelId = whatsapp.id)

      val list = repository.observeCustomChannels().first()

      assertEquals(listOf(inPerson.id, telegram.id, whatsapp.id, other.id), list.map { it.channel.id })
      assertEquals(listOf(0, 0, 2, 0), list.map { it.entryCount })
    }

  @Test
  fun openTasksAreDatedOldestFirstThenUndated() =
    runTest {
      val person = db.addPerson("Marta")
      val undated = db.addTask(person.id, text = "undated")
      val later = db.addTask(person.id, dueDate = 20_010)
      val sooner = db.addTask(person.id, dueDate = 20_000)
      db.addTask(person.id, doneAt = 5)

      assertEquals(
        listOf(sooner.id, later.id, undated.id),
        repository.observeOpenTasks(person.id).first().map { it.id },
      )
    }

  @Test
  fun savingTrimsTheTextAndEditingKeepsTheId() =
    runTest {
      val person = db.addPerson("Marta")

      val id = repository.saveTask(TaskEntity(personId = person.id, text = "  ask  ", createdAt = 1))
      repository.saveTask(repository.getTask(id)!!.copy(text = "ask again", dueDate = 20_000))

      val task = db.tasks.value.single()
      assertEquals(id, task.id)
      assertEquals("ask again", task.text)
      assertEquals(20_000L, task.dueDate)
    }

  @Test
  fun markingAsDoneMovesItToDoneAndReopeningBringsItBack() =
    runTest {
      val person = db.addPerson("Marta")
      val task = db.addTask(person.id)

      repository.markTaskDone(task, at = 7_000)
      assertTrue(repository.observeOpenTasks(person.id).first().isEmpty())
      assertEquals(listOf(7_000L), repository.observeDoneTasks(person.id).first().map { it.doneAt })

      repository.reopenTask(task)
      assertEquals(listOf(task.id), repository.observeOpenTasks(person.id).first().map { it.id })
      assertTrue(repository.observeDoneTasks(person.id).first().isEmpty())
    }

  @Test
  fun deletingATaskRemovesIt() =
    runTest {
      val person = db.addPerson("Marta")
      val task = db.addTask(person.id)

      repository.deleteTask(task)

      assertTrue(db.tasks.value.isEmpty())
    }

  @Test
  fun theListCountsOpenTasksButNeverForProtectedPeople() =
    runTest {
      val open = db.addPerson("Marta")
      val secret = db.addPerson("Luna", isProtected = true)
      db.addTask(open.id)
      db.addTask(open.id)
      db.addTask(open.id, doneAt = 1)
      db.addTask(secret.id)

      val list = repository.observePeople().first().associateBy { it.person.id }

      assertEquals(2, list.getValue(open.id).openTasks)
      assertEquals(0, list.getValue(secret.id).openTasks)
    }
}

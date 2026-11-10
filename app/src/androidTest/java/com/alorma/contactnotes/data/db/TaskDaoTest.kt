package com.alorma.contactnotes.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** L'SQL real de les tasques (Pro) sobre Room en memòria (el `FakeDb` no el prova). */
class TaskDaoTest {
  private lateinit var db: ConversesDatabase
  private lateinit var tasks: TaskDao
  private lateinit var people: PersonDao

  @Before
  fun setUp() {
    db =
      Room
        .inMemoryDatabaseBuilder(
          InstrumentationRegistry.getInstrumentation().targetContext,
          ConversesDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
    tasks = db.taskDao()
    people = db.personDao()
  }

  @After
  fun tearDown() = db.close()

  private suspend fun person(name: String = "Anna"): Long =
    people.insert(PersonEntity(name = name, createdAt = 0, updatedAt = 0))

  private suspend fun task(
    personId: Long,
    dueDate: Long? = null,
    createdAt: Long = 0,
    doneAt: Long? = null,
  ): Long =
    tasks.insert(TaskEntity(personId = personId, text = "t", dueDate = dueDate, createdAt = createdAt, doneAt = doneAt))

  @Test
  fun openTasksAreDatedOldestFirstThenUndatedSkippingDoneAndOtherPeople() =
    runTest {
      val p = person()
      val other = person("Bru")
      val undated = task(p, createdAt = 1)
      val later = task(p, dueDate = 20_010)
      val sooner = task(p, dueDate = 20_000)
      task(p, dueDate = 19_000, doneAt = 5)
      task(other, dueDate = 1)

      assertEquals(listOf(sooner, later, undated), tasks.observeOpen(p).first().map { it.id })
    }

  @Test
  fun doneTasksAreNewestFirstAndUndoBringsThemBack() =
    runTest {
      val p = person()
      val first = task(p)
      val second = task(p)

      tasks.setDoneAt(first, 10, now = 10)
      tasks.setDoneAt(second, 20, now = 20)
      assertEquals(listOf(second, first), tasks.observeDone(p).first().map { it.id })

      tasks.setDoneAt(first, null, now = 30)
      assertNull(tasks.get(first)!!.doneAt)
      assertEquals(listOf(first), tasks.observeOpen(p).first().map { it.id })
    }

  @Test
  fun peopleListCountsOpenTasksOnly() =
    runTest {
      val p = person()
      val other = person("Bru")
      task(p)
      task(p, doneAt = 5)

      val counts = people.observeList(archived = false).first().associate { it.person.id to it.openTasks }

      assertEquals(1, counts[p])
      assertEquals(0, counts[other])
    }

  @Test
  fun deletingAPersonDeletesItsTasks() =
    runTest {
      val p = person()
      task(p)

      people.delete(people.get(p)!!)

      assertEquals(emptyList<TaskEntity>(), tasks.observeOpen(p).first())
    }
}

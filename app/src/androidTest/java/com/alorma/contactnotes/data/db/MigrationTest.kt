package com.alorma.contactnotes.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MigrationTest {
  @get:Rule
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      ConversesDatabase::class.java,
    )

  private fun SupportSQLiteDatabase.insertPerson(id: Long) =
    execSQL(
      "INSERT INTO person (id, name, isProtected, archived, createdAt, updatedAt) VALUES ($id, 'P$id', 0, 0, 0, 0)",
    )

  private fun SupportSQLiteDatabase.insertChannel(
    personId: Long,
    type: String,
  ) = execSQL("INSERT INTO channel (personId, type) VALUES ($personId, '$type')")

  private fun SupportSQLiteDatabase.insertEntry(
    personId: Long,
    channelType: String,
  ) = execSQL(
    "INSERT INTO entry (personId, kind, channelType, text, occurredAt) VALUES ($personId, 'SAID', '$channelType', 't', 0)",
  )

  private fun SupportSQLiteDatabase.strings(sql: String): List<String> =
    query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

  @Test
  fun migrate1To2MergesEveryChannelIntoItsGroup() {
    helper.createDatabase(TEST_DB, 1).use { db ->
      db.insertPerson(1)
      listOf(
        "IN_PERSON",
        "WHATSAPP",
        "TELEGRAM",
        "X",
        "INSTAGRAM",
        "PHONE",
        "EMAIL",
        "DISCORD",
        "SIGNAL",
        "LINKEDIN",
        "REDDIT",
      ).forEach { db.insertEntry(1, it) }
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migration1To2())

    assertEquals(
      listOf("IN_PERSON", "CHAT", "CHAT", "SOCIAL", "SOCIAL", "PHONE", "CHAT", "CHAT", "CHAT", "SOCIAL", "SOCIAL"),
      db.strings("SELECT channelType FROM entry ORDER BY id"),
    )
  }

  @Test
  fun migrate1To2DropsTheChannelTableButKeepsPeopleAndEntries() {
    helper.createDatabase(TEST_DB, 1).use { db ->
      db.insertPerson(1)
      listOf("WHATSAPP", "TELEGRAM", "X", "IN_PERSON").forEach { db.insertChannel(1, it) }
      db.insertEntry(1, "PHONE")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migration1To2())

    assertTrue(db.strings("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'channel'").isEmpty())
    assertEquals(listOf("P1"), db.strings("SELECT name FROM person"))
    assertEquals(listOf("PHONE"), db.strings("SELECT channelType FROM entry"))
    assertEquals(listOf("t"), db.strings("SELECT text FROM entry"))
  }

  private companion object {
    const val TEST_DB = "migration-test"
  }
}

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

  @Test
  fun migrate2To3KeepsEverythingAndLeavesPeopleUnlinked() {
    helper.createDatabase(TEST_DB, 2).use { db ->
      db.insertPerson(1)
      db.execSQL(
        "INSERT INTO person (id, name, alias, isProtected, archived, notes, createdAt, updatedAt) " +
          "VALUES (2, 'P2', 'Alias', 1, 1, 'n', 10, 20)",
      )
      db.insertEntry(1, "CHAT")
      db.execSQL("INSERT INTO pinned_fact (personId, label, value, position) VALUES (2, 'L', 'V', 0)")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration2To3())

    assertEquals(listOf("P1", "P2"), db.strings("SELECT name FROM person ORDER BY id"))
    assertEquals(
      listOf("Alias|1|1|n|10|20"),
      db.strings(
        "SELECT alias || '|' || isProtected || '|' || archived || '|' || notes || '|' || " +
          "createdAt || '|' || updatedAt FROM person WHERE id = 2",
      ),
    )
    assertEquals(listOf("CHAT"), db.strings("SELECT channelType FROM entry"))
    assertEquals(listOf("V"), db.strings("SELECT value FROM pinned_fact"))
    assertEquals(
      listOf("0"),
      db.strings(
        "SELECT COUNT(*) FROM person " +
          "WHERE contactId IS NOT NULL OR contactLookupKey IS NOT NULL OR contactName IS NOT NULL",
      ),
    )
  }

  @Test
  fun migrate3To4DropsTheContactColumnsAndKeepsPeopleEntriesAndFacts() {
    helper.createDatabase(TEST_DB, 3).use { db ->
      db.insertPerson(1)
      db.execSQL(
        "INSERT INTO person (id, name, alias, avatarShape, avatarColor, isProtected, archived, notes, createdAt, " +
          "updatedAt, contactId, contactLookupKey, contactName) " +
          "VALUES (2, 'P2', 'Alias', 'Sunny', 'Rose', 1, 1, 'n', 10, 20, 7, 'lookup', 'Contact name')",
      )
      db.insertEntry(1, "CHAT")
      db.insertEntry(2, "PHONE")
      db.execSQL("INSERT INTO pinned_fact (personId, label, value, position) VALUES (2, 'L', 'V', 0)")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

    assertEquals(listOf("P1", "P2"), db.strings("SELECT name FROM person ORDER BY id"))
    assertEquals(
      listOf("Alias|Sunny|Rose|1|1|n|10|20"),
      db.strings(
        "SELECT alias || '|' || avatarShape || '|' || avatarColor || '|' || isProtected || '|' || archived || '|' || " +
          "notes || '|' || createdAt || '|' || updatedAt FROM person WHERE id = 2",
      ),
    )
    assertEquals(
      listOf("1|CHAT", "2|PHONE"),
      db.strings("SELECT personId || '|' || channelType FROM entry ORDER BY id"),
    )
    assertEquals(listOf("2|V"), db.strings("SELECT personId || '|' || value FROM pinned_fact"))
    assertTrue(db.strings("SELECT name FROM pragma_table_info('person') WHERE name LIKE 'contact%'").isEmpty())
  }

  @Test
  fun migrate1To4RunsEveryMigration() {
    helper.createDatabase(TEST_DB, 1).use { db ->
      db.insertPerson(1)
      db.insertChannel(1, "WHATSAPP")
      db.insertEntry(1, "TELEGRAM")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration1To2(), Migration2To3())

    assertEquals(listOf("P1"), db.strings("SELECT name FROM person"))
    assertEquals(listOf("CHAT"), db.strings("SELECT channelType FROM entry"))
  }

  @Test
  fun migrate4To5KeepsEverythingAndLeavesEntriesWithoutChannelAndNoTasks() {
    helper.createDatabase(TEST_DB, 4).use { db ->
      db.insertPerson(1)
      db.execSQL(
        "INSERT INTO person (id, name, alias, avatarShape, avatarColor, isProtected, archived, notes, createdAt, " +
          "updatedAt) VALUES (2, 'P2', 'Alias', 'Sunny', 'Rose', 1, 1, 'n', 10, 20)",
      )
      db.execSQL(
        "INSERT INTO entry (id, personId, kind, channelType, text, occurredAt) VALUES (5, 1, 'HEARD', 'CHAT', 'a', 100)",
      )
      db.execSQL(
        "INSERT INTO entry (id, personId, kind, channelType, text, occurredAt) VALUES (9, 2, 'SAID', 'PHONE', 'b', 200)",
      )
      db.execSQL("INSERT INTO pinned_fact (personId, label, value, position) VALUES (2, 'L', 'V', 0)")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 5, true)

    assertEquals(listOf("P1", "P2"), db.strings("SELECT name FROM person ORDER BY id"))
    assertEquals(
      listOf("Alias|Sunny|Rose|1|1|n|10|20"),
      db.strings(
        "SELECT alias || '|' || avatarShape || '|' || avatarColor || '|' || isProtected || '|' || archived || '|' || " +
          "notes || '|' || createdAt || '|' || updatedAt FROM person WHERE id = 2",
      ),
    )
    assertEquals(
      listOf("5|1|HEARD|CHAT|a|100|-", "9|2|SAID|PHONE|b|200|-"),
      db.strings(
        "SELECT id || '|' || personId || '|' || kind || '|' || channelType || '|' || text || '|' || " +
          "occurredAt || '|' || IFNULL(customChannelId, '-') " +
          "FROM entry ORDER BY id",
      ),
    )
    assertEquals(listOf("2|V"), db.strings("SELECT personId || '|' || value FROM pinned_fact"))
    assertEquals(listOf("0"), db.strings("SELECT COUNT(*) FROM custom_channel"))
    assertEquals(listOf("0"), db.strings("SELECT COUNT(*) FROM task"))
  }

  @Test
  fun migrate4To5DeletingACustomChannelLeavesItsEntriesInTheGroup() {
    helper.createDatabase(TEST_DB, 4).use { db ->
      db.insertPerson(1)
      db.insertEntry(1, "CHAT")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 5, true)
    db.execSQL("PRAGMA foreign_keys = ON")
    db.execSQL("INSERT INTO custom_channel (id, channelType, name, createdAt) VALUES (3, 'CHAT', 'WhatsApp', 0)")
    db.execSQL("UPDATE entry SET customChannelId = 3")
    db.execSQL("DELETE FROM custom_channel WHERE id = 3")

    assertEquals(
      listOf("CHAT|-|t"),
      db.strings("SELECT channelType || '|' || IFNULL(customChannelId, '-') || '|' || text FROM entry"),
    )
  }

  @Test
  fun migrate4To5TasksAreDeletedWithTheirPerson() {
    helper.createDatabase(TEST_DB, 4).use { db ->
      db.insertPerson(1)
      db.insertPerson(2)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 5, true)
    db.execSQL("PRAGMA foreign_keys = ON")
    db.execSQL("INSERT INTO task (personId, text, dueDate, createdAt) VALUES (1, 'a', NULL, 0)")
    db.execSQL("INSERT INTO task (personId, text, dueDate, createdAt) VALUES (2, 'b', 20000, 0)")
    db.execSQL("DELETE FROM person WHERE id = 1")

    assertEquals(
      listOf("b|20000|0|-"),
      db.strings("SELECT text || '|' || dueDate || '|' || remind || '|' || IFNULL(doneAt, '-') FROM task"),
    )
  }

  @Test
  fun migrate1To5RunsEveryMigration() {
    helper.createDatabase(TEST_DB, 1).use { db ->
      db.insertPerson(1)
      db.insertChannel(1, "WHATSAPP")
      db.insertEntry(1, "TELEGRAM")
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration1To2(), Migration2To3())

    assertEquals(listOf("P1"), db.strings("SELECT name FROM person"))
    assertEquals(listOf("CHAT"), db.strings("SELECT channelType FROM entry"))
    assertEquals(listOf("0"), db.strings("SELECT COUNT(*) FROM task"))
  }

  private companion object {
    const val TEST_DB = "migration-test"
  }
}

package com.alorma.contactnotes.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec

@Database(
  entities = [
    PersonEntity::class,
    EntryEntity::class,
    PinnedFactEntity::class,
  ],
  version = 4,
  autoMigrations = [
    AutoMigration(from = 3, to = 4, spec = ConversesDatabase.DropContactLink::class),
  ],
)
@TypeConverters(AvatarShapeConverter::class)
abstract class ConversesDatabase : RoomDatabase() {
  abstract fun personDao(): PersonDao

  abstract fun entryDao(): EntryDao

  abstract fun pinnedFactDao(): PinnedFactDao

  /**
   * v3 → v4: es retira el vincle amb Contactes. Room recrea `person` sense les tres columnes; la resta de dades i les
   * entrades i fets de cada persona es conserven. No es treu mai: qualsevol BBDD a la v3 ha de passar per aquí.
   */
  @DeleteColumn(tableName = "person", columnName = "contactId")
  @DeleteColumn(tableName = "person", columnName = "contactLookupKey")
  @DeleteColumn(tableName = "person", columnName = "contactName")
  class DropContactLink : AutoMigrationSpec
}

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
    CustomChannelEntity::class,
  ],
  version = 5,
  autoMigrations = [
    AutoMigration(from = 3, to = 4, spec = ConversesDatabase.DropContactLink::class),
    // v4 → v5: canals propis i pendents (Pro). Taula `custom_channel` nova; `entry` es recrea amb la clau forana nova
    // i les columnes `customChannelId` (null), `isPending` (0) i `doneAt` (null). Cap dada no es perd.
    AutoMigration(from = 4, to = 5),
  ],
)
@TypeConverters(AvatarShapeConverter::class)
abstract class ConversesDatabase : RoomDatabase() {
  abstract fun personDao(): PersonDao

  abstract fun entryDao(): EntryDao

  abstract fun pinnedFactDao(): PinnedFactDao

  abstract fun customChannelDao(): CustomChannelDao

  /**
   * v3 → v4: es retira el vincle amb Contactes. Room recrea `person` sense les tres columnes; la resta de dades i les
   * entrades i fets de cada persona es conserven. No es treu mai: qualsevol BBDD a la v3 ha de passar per aquí.
   */
  @DeleteColumn(tableName = "person", columnName = "contactId")
  @DeleteColumn(tableName = "person", columnName = "contactLookupKey")
  @DeleteColumn(tableName = "person", columnName = "contactName")
  class DropContactLink : AutoMigrationSpec
}

package com.alorma.contactnotes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [
    PersonEntity::class,
    EntryEntity::class,
    PinnedFactEntity::class,
  ],
  version = 2,
)
@TypeConverters(AvatarShapeConverter::class)
abstract class ConversesDatabase : RoomDatabase() {
  abstract fun personDao(): PersonDao
  abstract fun entryDao(): EntryDao
  abstract fun pinnedFactDao(): PinnedFactDao
}

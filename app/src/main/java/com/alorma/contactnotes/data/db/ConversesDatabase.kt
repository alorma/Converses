package com.alorma.contactnotes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
  entities = [
    PersonEntity::class,
    ChannelEntity::class,
    EntryEntity::class,
    PinnedFactEntity::class,
  ],
  version = 2,
)
abstract class ConversesDatabase : RoomDatabase() {
  abstract fun personDao(): PersonDao
  abstract fun channelDao(): ChannelDao
  abstract fun entryDao(): EntryDao
  abstract fun pinnedFactDao(): PinnedFactDao
}

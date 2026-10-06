package com.alorma.contactnotes.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.insertSampleData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun database(
    @ApplicationContext context: Context,
    databaseProvider: Provider<ConversesDatabase>,
  ): ConversesDatabase {
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    return Room.databaseBuilder(context, ConversesDatabase::class.java, "converses.db")
      .addCallback(
        object : RoomDatabase.Callback() {
          override fun onCreate(connection: SupportSQLiteDatabase) {
            if (isDebuggable) {
              CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                databaseProvider.get().insertSampleData()
              }
            }
          }
        },
      )
      .build()
  }

  @Provides fun personDao(db: ConversesDatabase) = db.personDao()
  @Provides fun channelDao(db: ConversesDatabase) = db.channelDao()
  @Provides fun entryDao(db: ConversesDatabase) = db.entryDao()
  @Provides fun pinnedFactDao(db: ConversesDatabase) = db.pinnedFactDao()
}

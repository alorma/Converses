package com.alorma.contactnotes.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.insertSampleData
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.settings.DataStoreSettingsRepository
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.ui.edit.EditPersonViewModel
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteViewModel
import com.alorma.contactnotes.ui.people.PeopleListViewModel
import com.alorma.contactnotes.ui.settings.SettingsViewModel
import com.alorma.contactnotes.ui.person.PersonDetailViewModel
import com.alorma.contactnotes.ui.person.PersonExporter
import com.alorma.contactnotes.ui.person.PersonViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val databaseModule = module {
  single<ConversesDatabase> {
    val context: Context = androidContext()
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    Room.databaseBuilder(context, ConversesDatabase::class.java, "converses.db")
      .addCallback(
        object : RoomDatabase.Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            if (isDebuggable) {
              CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                get<ConversesDatabase>().insertSampleData()
              }
            }
          }
        },
      )
      .apply { if (isDebuggable) fallbackToDestructiveMigration(dropAllTables = true) }
      .build()
  }
  single { get<ConversesDatabase>().personDao() }
  single { get<ConversesDatabase>().channelDao() }
  single { get<ConversesDatabase>().entryDao() }
  single { get<ConversesDatabase>().pinnedFactDao() }
  singleOf(::ConversesRepository)
}

val settingsModule = module {
  single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
  single { AppLockManager(get()) }
  single { SecureWindow() }
  single { PersonExporter(androidContext(), get()) }
  single { PersonSelectionBridge() }
}

val viewModelModule = module {
  viewModel { (archived: Boolean) -> PeopleListViewModel(archived, get()) }
  viewModelOf(::SettingsViewModel)
  viewModel { (personId: Long) -> PersonViewModel(personId, get()) }
  viewModel { (personId: Long) -> PersonDetailViewModel(personId, get(), get()) }
  viewModel { (personId: Long) -> QuickNoteViewModel(personId, get(), get()) }
  viewModel { (personId: Long) -> EditPersonViewModel(personId, get(), get()) }
}

val appModules = listOf(databaseModule, settingsModule, viewModelModule)

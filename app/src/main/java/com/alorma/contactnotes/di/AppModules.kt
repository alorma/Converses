package com.alorma.contactnotes.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.db.Migration1To2
import com.alorma.contactnotes.data.flags.CustomAvatarFeature
import com.alorma.contactnotes.data.flags.FeatureFlag
import com.alorma.contactnotes.data.flags.FeatureFlagRunner
import com.alorma.contactnotes.data.flags.FeatureFlagStore
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.flags.PersonColorFeature
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.settings.DataStoreFireAndForgetRunner
import com.alorma.contactnotes.data.settings.DataStoreSettingsRepository
import com.alorma.contactnotes.data.settings.OnboardingFlag
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.data.wear.DataStoreNoteIdRegistry
import com.alorma.contactnotes.data.wear.GmsWearLink
import com.alorma.contactnotes.data.wear.NoteIdRegistry
import com.alorma.contactnotes.data.wear.WearEntriesResponder
import com.alorma.contactnotes.data.wear.WearLink
import com.alorma.contactnotes.data.wear.WearNoteReceiver
import com.alorma.contactnotes.data.wear.WearSnapshotPublisher
import com.alorma.contactnotes.ui.edit.EditPersonViewModel
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteViewModel
import com.alorma.contactnotes.ui.people.PeopleListViewModel
import com.alorma.contactnotes.ui.person.PersonDetailViewModel
import com.alorma.contactnotes.ui.person.PersonExporter
import com.alorma.contactnotes.ui.person.PersonLockViewModel
import com.alorma.contactnotes.ui.person.PersonViewModel
import com.alorma.contactnotes.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val databaseModule =
  module {
    single<ConversesDatabase> {
      val context: Context = androidContext()
      val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
      Room
        .databaseBuilder(context, ConversesDatabase::class.java, "converses.db")
        // S'executa en un fil de fons i només amb una BBDD v1: marca l'avís dels canals (les instal·lacions noves no hi passen).
        .addMigrations(Migration1To2 { runBlocking { get<SettingsRepository>().setChannelsNoticePending(true) } })
        .apply { if (isDebuggable) fallbackToDestructiveMigration(dropAllTables = true) }
        .build()
    }
    single { get<ConversesDatabase>().personDao() }
    single { get<ConversesDatabase>().entryDao() }
    single { get<ConversesDatabase>().pinnedFactDao() }
    singleOf(::ConversesRepository)
  }

val settingsModule =
  module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single { OnboardingFlag(DataStoreFireAndForgetRunner(get())) }
    single { AppLockManager(get()) }
    single { SecureWindow() }
    single { FeatureFlagStore(androidContext()) }
    single {
      val store: FeatureFlagStore = get()
      LocalFeatureFlagRunner(initial = store.load(), persist = store::save)
    } bind FeatureFlagRunner::class
    singleOf(::CustomAvatarFeature) bind FeatureFlag::class
    singleOf(::PersonColorFeature) bind FeatureFlag::class
    single { PersonExporter(androidContext(), get()) }
    single { PersonSelectionBridge() }
  }

val viewModelModule =
  module {
    viewModel { (archived: Boolean) -> PeopleListViewModel(archived, get(), get()) }
    viewModelOf(::SettingsViewModel)
    viewModel { (personId: Long) -> PersonViewModel(personId, get()) }
    viewModel { PersonLockViewModel() }
    viewModel { (personId: Long) -> PersonDetailViewModel(personId, get(), get()) }
    viewModel { (personId: Long) -> QuickNoteViewModel(personId, get(), get()) }
    viewModel { (personId: Long) -> EditPersonViewModel(personId, get(), get()) }
  }

val wearModule =
  module {
    single<WearLink> { GmsWearLink(androidContext()) }
    single { WearEntriesResponder(repository = get()) }
    single<NoteIdRegistry> { DataStoreNoteIdRegistry(androidContext()) }
    single { WearNoteReceiver(repository = get(), registry = get()) }
    single {
      WearSnapshotPublisher(
        repository = get(),
        settings = get(),
        link = get(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
      )
    }
  }

val appModules = listOf(databaseModule, settingsModule, wearModule, viewModelModule)

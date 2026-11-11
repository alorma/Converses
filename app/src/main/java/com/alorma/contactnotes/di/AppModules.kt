package com.alorma.contactnotes.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.db.Migration1To2
import com.alorma.contactnotes.data.db.Migration2To3
import com.alorma.contactnotes.data.db.Migration5To6
import com.alorma.contactnotes.data.flags.FeatureFlag
import com.alorma.contactnotes.data.flags.FeatureFlagRunner
import com.alorma.contactnotes.data.flags.FeatureFlagStore
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.flags.ProFeatureFlagRunner
import com.alorma.contactnotes.data.pro.AndroidNetworkMonitor
import com.alorma.contactnotes.data.pro.NetworkMonitor
import com.alorma.contactnotes.data.pro.ProRepository
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.settings.DataStoreFireAndForgetRunner
import com.alorma.contactnotes.data.settings.DataStoreSettingsRepository
import com.alorma.contactnotes.data.settings.OnboardingFlag
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.data.sync.AccountRemote
import com.alorma.contactnotes.data.sync.AndroidSyncKeyStore
import com.alorma.contactnotes.data.sync.AuthGateway
import com.alorma.contactnotes.data.sync.DataStoreSyncLocalStore
import com.alorma.contactnotes.data.sync.FirebaseAuthGateway
import com.alorma.contactnotes.data.sync.FirestoreAccountRemote
import com.alorma.contactnotes.data.sync.SyncFlag
import com.alorma.contactnotes.data.sync.SyncKeyStore
import com.alorma.contactnotes.data.sync.SyncLocalStore
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.tracking.AndroidFirebaseGateway
import com.alorma.contactnotes.data.tracking.ConsentManager
import com.alorma.contactnotes.data.tracking.EventTracker
import com.alorma.contactnotes.data.tracking.FirebaseController
import com.alorma.contactnotes.data.tracking.FirebaseGateway
import com.alorma.contactnotes.data.tracking.ProTrackingProperty
import com.alorma.contactnotes.ui.edit.EditPersonViewModel
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteViewModel
import com.alorma.contactnotes.ui.paywall.PaywallViewModel
import com.alorma.contactnotes.ui.people.PeopleListViewModel
import com.alorma.contactnotes.ui.person.PersonDetailViewModel
import com.alorma.contactnotes.ui.person.PersonExporter
import com.alorma.contactnotes.ui.person.PersonLockViewModel
import com.alorma.contactnotes.ui.person.PersonViewModel
import com.alorma.contactnotes.ui.settings.CustomChannelsViewModel
import com.alorma.contactnotes.ui.settings.SettingsViewModel
import com.alorma.contactnotes.ui.sync.SyncViewModel
import com.alorma.contactnotes.ui.task.TaskFormViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        .addMigrations(Migration1To2(), Migration2To3(), Migration5To6())
        .apply { if (isDebuggable) fallbackToDestructiveMigration(dropAllTables = true) }
        .build()
    }
    single { get<ConversesDatabase>().personDao() }
    single { get<ConversesDatabase>().entryDao() }
    single { get<ConversesDatabase>().pinnedFactDao() }
    single { get<ConversesDatabase>().customChannelDao() }
    single { get<ConversesDatabase>().taskDao() }
    // Sense `singleOf`: intentaria resoldre el rellotge, que té valor per defecte
    single { ConversesRepository(get(), get(), get(), get(), get()) }
  }

val settingsModule =
  module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single { OnboardingFlag(DataStoreFireAndForgetRunner(get())) }
    single { AppLockManager(get(), get()) }
    single { SecureWindow() }
    single { FeatureFlagStore(androidContext()) }
    single {
      val store: FeatureFlagStore = get()
      LocalFeatureFlagRunner(initial = store.load(), persist = store::save)
    }
    singleOf(::ProRepository)
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    single<FeatureFlagRunner> {
      ProFeatureFlagRunner(
        flags = get<LocalFeatureFlagRunner>(),
        pro = get(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
      )
    }
    single { PersonExporter(androidContext(), get()) }
    single { PersonSelectionBridge() }
    // No es lliga a Tracker: si ho fes, getAll<Tracker>() el trobaria a ell mateix
    single { ConsentManager(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single {
      EventTracker(
        trackers = getAll(),
        consent = get<ConsentManager>().consent,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
      )
    }
    single { ProTrackingProperty(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single<FirebaseGateway> { AndroidFirebaseGateway(androidContext()) }
    single {
      FirebaseController(
        gateway = get(),
        consent = get<ConsentManager>().consent,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
      )
    }
  }

/** La sincronització al núvol (`docs/sync/disseny.md`). Res d'aquí es crea fins que algú el demana amb [SyncFlag] encès. */
val syncModule =
  module {
    single { SyncFlag(get()) } bind FeatureFlag::class
    single<AuthGateway> { FirebaseAuthGateway(androidContext()) }
    single<AccountRemote> { FirestoreAccountRemote(androidContext()) }
    single<SyncKeyStore> { AndroidSyncKeyStore(androidContext()) }
    single<SyncLocalStore> { DataStoreSyncLocalStore(androidContext()) }
    // Sense `singleOf`: intentaria resoldre el rellotge i l'aleatori, que tenen valor per defecte
    single { SyncRepository(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
  }

val viewModelModule =
  module {
    viewModelOf(::SyncViewModel)
    viewModel { (archived: Boolean) -> PeopleListViewModel(archived, get(), get()) }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::CustomChannelsViewModel)
    viewModelOf(::PaywallViewModel)
    viewModel { (personId: Long) -> PersonViewModel(personId, get()) }
    viewModel { PersonLockViewModel() }
    viewModel { (personId: Long) -> PersonDetailViewModel(personId, get(), get(), get()) }
    viewModel { (personId: Long) -> QuickNoteViewModel(personId, get(), get(), get()) }
    viewModel { (personId: Long, taskId: Long) -> TaskFormViewModel(personId, taskId, get(), get(), get()) }
    viewModel { (personId: Long) -> EditPersonViewModel(personId, get(), get(), get()) }
  }

val appModules =
  listOf(
    databaseModule,
    proDataSourceModule,
    trackersModule,
    settingsModule,
    syncModule,
    viewModelModule,
  )

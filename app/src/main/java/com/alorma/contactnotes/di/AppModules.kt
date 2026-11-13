package com.alorma.contactnotes.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
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
import com.alorma.contactnotes.data.sync.DataStoreDeviceStore
import com.alorma.contactnotes.data.sync.DataStoreSyncCursorStore
import com.alorma.contactnotes.data.sync.DataStoreSyncLocalStore
import com.alorma.contactnotes.data.sync.DeviceStore
import com.alorma.contactnotes.data.sync.EngineSyncRunner
import com.alorma.contactnotes.data.sync.FirebaseAuthGateway
import com.alorma.contactnotes.data.sync.FirestoreAccountRemote
import com.alorma.contactnotes.data.sync.FirestoreSyncRemote
import com.alorma.contactnotes.data.sync.LocalSyncSource
import com.alorma.contactnotes.data.sync.RoomLocalSyncSource
import com.alorma.contactnotes.data.sync.SyncCursorStore
import com.alorma.contactnotes.data.sync.SyncData
import com.alorma.contactnotes.data.sync.SyncEngine
import com.alorma.contactnotes.data.sync.SyncFirebase
import com.alorma.contactnotes.data.sync.SyncFlag
import com.alorma.contactnotes.data.sync.SyncKeyStore
import com.alorma.contactnotes.data.sync.SyncLocalStore
import com.alorma.contactnotes.data.sync.SyncRemote
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncScheduler
import com.alorma.contactnotes.data.sync.SyncWorker
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
import com.alorma.contactnotes.ui.sync.EnterWordsViewModel
import com.alorma.contactnotes.ui.sync.SecurityWordsViewModel
import com.alorma.contactnotes.ui.sync.SlotsViewModel
import com.alorma.contactnotes.ui.sync.SyncPromptsViewModel
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
    single { get<ConversesDatabase>().syncTombstoneDao() }
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
    // El proveïdor d'App Check (`syncAppCheckModule`, un per tipus de build) no es demana fins que la instància es crea
    single { SyncFirebase(androidContext()) { get() } }
    single<AuthGateway> { FirebaseAuthGateway(androidContext(), get(), get<EventTracker>()) }
    single<AccountRemote> { FirestoreAccountRemote(get(), get<EventTracker>()) }
    single<SyncKeyStore> { AndroidSyncKeyStore(androidContext(), get<EventTracker>()) }
    single<SyncLocalStore> { DataStoreSyncLocalStore(androidContext()) }
    single<DeviceStore> { DataStoreDeviceStore(androidContext()) }
    single<SyncCursorStore> { DataStoreSyncCursorStore(androidContext(), get()) }
    single<SyncRemote> { FirestoreSyncRemote(get(), get<EventTracker>()) }
    single<LocalSyncSource> { RoomLocalSyncSource(get(), get(), get(), get(), get(), get()) }
    single {
      val tracker = get<EventTracker>()
      SyncEngine(get(), get(), get(), onError = { error, where -> tracker.trackError(error, where) })
    }
    // Sense `singleOf`: intentaria resoldre el rellotge i l'aleatori, que tenen valor per defecte
    single {
      val context = androidContext()
      val data =
        SyncData(
          runner = EngineSyncRunner(get()),
          source = get(),
          records = get(),
          cursors = get(),
          devices = get(),
          deviceName = {
            Settings.Global.getString(
              context.contentResolver,
              Settings.Global.DEVICE_NAME,
            ) ?: Build.MODEL
          },
          isPro = { get<ProRepository>().isPro.value },
          isProChanges = get<ProRepository>().isPro,
          onError = { error, where -> get<EventTracker>().trackError(error, where) },
        )
      SyncRepository(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default), sync = data)
    }
    single {
      SyncScheduler(
        get(),
        { SyncWorker.enqueue(androidContext()) },
        CoroutineScope(
          SupervisorJob() + Dispatchers.Default,
        ),
      )
    }
  }

val viewModelModule =
  module {
    viewModel { SyncViewModel(get(), get()) }
    viewModelOf(::SyncPromptsViewModel)
    viewModelOf(::SlotsViewModel)
    viewModel { (activation: Boolean) -> SecurityWordsViewModel(activation, get(), get()) }
    viewModelOf(::EnterWordsViewModel)
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
    syncAppCheckModule,
    viewModelModule,
  )

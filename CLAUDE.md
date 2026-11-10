# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Converses

Android app to remember what I said, what people said to me and what happened with each person (chat, social networks or in person).

## References (source of truth)

The docs are written in Catalan and describe what exists and why, not how it was built (that is in git history).

- `docs/backlog.md`: pending bugs and features.
- `docs/pro/README.md`: what is free forever and what is Pro, how Pro works (RevenueCat, `ProRepository`, downgrade).
- `docs/pantalles-grans/README.md`: how the app adapts to tablets and foldables (rail, list and detail side by side, forms as dialogs, the hinge) and why; the design reference is in `docs/pantalles-grans/disseny/`.
- `docs/analytics/README.md`: opt-in usage statistics (not Pro): privacy rules, consent, the agnostic `Tracker`, the event catalogs and Firebase; Firebase is never initialized without consent.

## Stack

Kotlin, Jetpack Compose, Material 3 Expressive (`material3` 1.5 alpha: in the stable 1.4 the Expressive APIs are internal), Material 3 Adaptive, MaterialKolor, Room with KSP, Koin, Navigation 3, DataStore.

## Commands

Two modules: `:app` and `:designsystem`. ViewModel and repository tests are plain JVM (no Robolectric, no Room); there are no instrumented UI tests (a deliberate decision, they complicate the CI).

- Unit tests (what the CI runs): `./gradlew testDebugUnitTest`
- A single test: `./gradlew :app:testDebugUnitTest --tests '*PeopleListViewModelTest'`
- Debug APK: `./gradlew :app:assembleDebug`
- Validate screenshots: `./gradlew validateDebugScreenshotTest`
- Regenerate screenshots: `./gradlew updateDebugScreenshotTest`; for a single test: `./gradlew :app:updateDebugScreenshotTest --tests '*ScreensScreenshotTests.peopleListLoaded'`

Code style is enforced with ktlint (`./gradlew ktlintCheck`, `./gradlew ktlintFormat` to fix it); rules live in `.editorconfig`.

## Architecture

- **Single activity** (`MainActivity`, a `FragmentActivity` because of `BiometricPrompt`) with Navigation 3. Routes are serializable `NavKey`s (`PeopleRoute`, `PersonRoute(id)`, `PersonLockedRoute(id)`, `QuickNoteRoute`, `EditPersonRoute`…) declared in `ui/navigation/Routes.kt` and wired up in `ui/AppNavigation.kt`. There is a single saveable back stack managed by `TopLevelBackStack` (`navigate`, `switchTab`, `pop`, `resetTo`): People and Archived are tabs (`TopLevelRoute`) that keep no history, everything else stacks on top, and back at the base closes the app.
- **Large screens:** the layout follows the window, never the device model or the orientation. Below 600 dp it is the phone layout and must not change. From 600 dp `NavDisplay` gets two scene strategies: `ListDetailSceneStrategy` shows the list routes (`PeopleRoute`, `ArchivedRoute`) and the person routes (`PersonRoute`, `PersonLockedRoute`) side by side, and `DialogSceneStrategy` shows the form routes (`isFormRoute()`: quick note, person editor, task, Pro) as 560 dp dialogs. The routes and the back stack are the same at every width, so resizing, rotating or folding keeps the open person and what was being typed. Screens learn where they are through an `inPane` parameter (no back arrow, pane background); the person detail goes to two columns when its own pane is at least 600 dp wide. An open foldable (a vertical hinge in the window) splits the panes at the hinge and has no rail. A form dialog is its own window: `FormDialogFrame` gives it `FLAG_SECURE` whenever `SecureWindow` asks for it.
- **Layers:** `data/` (Room in `data/db`, `ConversesRepository`, DataStore in `data/settings`), `di/AppModules.kt` (Koin) and `ui/<screen>/` with one `ViewModel` and one screen per feature. Each screen exposes a stateless version (`...Content`) used by previews and screenshot tests.
- **`:designsystem`** holds the theme (`ConversesTheme`, `PersonTheme`, colors, Roboto Flex) and the custom components; it is the only place with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
- **Protected people:** a cross-cutting concern. `PersonLockedScreen` only receives name, shape and color and loads nothing until `BiometricPrompt` succeeds; `FLAG_SECURE` (`SecureWindow`) is set on screens that show a protected person's data; the database and DataStore are excluded from Android backup. Any new feature (list, shortcuts, export, quick note) must not leak a protected person's data before unlocking.
- **Person color:** a person's screens (detail, editor, quick note with a person) take the color of their avatar via `PersonTheme`; the list, Archived and Settings use the app theme.
- **`EntryForm`** is the only entry form (quick note, new entry and edit). `whoLabel(...)` is the only function producing the labels Me / {first name} / Note; `EntryKind` (`SAID`, `HEARD`, `HAPPENED`) does not change.
- **Room:** version 1 is published on Google Play (beta track), schema in `app/schemas`. **Every database change MUST ship with a migration:** bump `version` in `ConversesDatabase`, commit the new exported schema JSON, add a `Migration` (or `AutoMigration`) registered in `di/AppModules.kt` and a migration test with `MigrationTestHelper` (`room-testing` is already in `androidTestImplementation`). Never edit an already-exported schema or rely on `fallbackToDestructiveMigration`: it is enabled only in debug and would wipe users' data in release. Sample data is not seeded by default, only from the debug menu (`insertSampleData`).
- **Sync identity** (`docs/sync/disseny.md`, the only part of sync that exists): people, entries, tasks and custom channels carry `syncId`, `dirty` and a modification time (`modifiedAt`; `updatedAt` for a person), and deletions leave a row in `sync_tombstone`. Every new write in `ConversesRepository` must stamp the row (`stamped(clock())`), every bulk `UPDATE` must set `modifiedAt` and `dirty = 1` in SQL, and every delete must go through a DAO transaction that writes the tombstone (the plain `delete` of each DAO is `protected`). `SyncIdentityTest` fails when a public write method of the repository is not classified there.
- **Tests:** `FakeDb` (in-memory DAOs) and `MainDispatcherRule` in `app/src/test/.../testing`. Screenshot test method names use camelCase with no `_`; references are versioned in `<module>/src/screenshotTestDebug/reference/`.
- **CI** (`.github/workflows`): `pr-build.yml` (ktlint + tests + debug APK), `build-apk.yml`, `screenshot-tests.yml`, `release.yml` (manual, with a `track` input) and `publish-listing.yml`. The `update-screenshots` label on a PR regenerates the references; there is no need to generate them locally.

## Rules

- Work in small steps and validate each one before continuing; pending work lives in `docs/backlog.md`.
- Before adding a library, check its current stable version in the official documentation.
- Every screen is a Material 3 `Scaffold`: fixed headers (back, title, actions) go in `topBar` and never scroll with the content (titled screens use `ConversesTopBar` from `:designsystem`: Medium by default, `large = true` for Settings; person detail and the people list keep their own headers), screen FABs in `floatingActionButton`, snackbars in `snackbarHost`; no manual `windowInsetsPadding` for the system bars, use the `padding` the `Scaffold` gives (with `consumeWindowInsets` before `imePadding`). The main navigation does not belong to any screen: it lives once in `AppShell` (`ui/navigation`), a parent `Scaffold` around `NavDisplay`, and `shellNavigationFor` decides what it draws. On the phone it is the floating toolbar, in the FAB slot (`FabPosition.Center`), not in `bottomBar`, because it must not reserve space; it is only visible when the top of the stack is a tab (`TopLevelRoute`). From 600 dp it is a rail on the left (`ConversesNavigationRail`), which does reserve its space and stays under the form dialogs. On an open foldable there is no rail: the floating toolbar is drawn at the bottom of the list pane (`floatingBarWidth`). Lists leave room for the toolbar with `LocalFloatingBarPadding`, never a fixed `dp`. Reading columns (Settings, Pro, onboarding) use `Modifier.readableWidth()` to stay centered at 640 dp at most.
- Keep all `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` in the `designsystem` module; screens do not repeat them.
- UI text always goes in `strings.xml`: English by default (`values/`), with Catalan (`values-ca/`) and Spanish (`values-es/`).
- Gradle with a version catalog (`gradle/libs.versions.toml`).
- The app is released on Google Play: any Room schema change (entities, columns, indices, DAOs' tables) requires a version bump, a migration and its test. No exceptions.

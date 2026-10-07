# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Converses

Android app to remember what I said, what people said to me and what happened with each person (chat, social networks or in person).

## References (source of truth)

- `docs/plan.md`: state of the app and the plan (stack, theme, components, screens, data model, decisions and pending work). Keep it up to date at the end of every task. It is written in Catalan.
- `docs/backlog.md`: pending bugs and features (Iteration 2).
- `docs/onboarding/tanda-4-onboarding.md`: onboarding specification; if it differs from the plan, this document wins.

## Stack

Kotlin, Jetpack Compose, Material 3 Expressive (`material3` 1.5 alpha: in the stable 1.4 the Expressive APIs are internal), MaterialKolor, Room with KSP, Koin, Navigation 3, DataStore.

## Commands

Two modules: `:app` and `:designsystem`. ViewModel and repository tests are plain JVM (no Robolectric, no Room); there are no instrumented UI tests (a deliberate decision, they complicate the CI).

- Unit tests (what the CI runs): `./gradlew testDebugUnitTest`
- A single test: `./gradlew :app:testDebugUnitTest --tests '*PeopleListViewModelTest'`
- Debug APK: `./gradlew :app:assembleDebug`
- Validate screenshots: `./gradlew validateDebugScreenshotTest`
- Regenerate screenshots: `./gradlew updateDebugScreenshotTest`; for a single test: `./gradlew :app:updateDebugScreenshotTest --tests '*ScreensScreenshotTests.peopleListLoaded'`

No linter (ktlint/detekt) is configured.

## Architecture

- **Single activity** (`MainActivity`, a `FragmentActivity` because of `BiometricPrompt`) with Navigation 3. Routes are serializable `NavKey`s (`PeopleRoute`, `PersonRoute(id)`, `PersonLockedRoute(id)`, `QuickNoteRoute`, `EditPersonRoute`…) wired up in `ui/AppNavigation.kt`.
- **Layers:** `data/` (Room in `data/db`, `ConversesRepository`, DataStore in `data/settings`), `di/AppModules.kt` (Koin) and `ui/<screen>/` with one `ViewModel` and one screen per feature. Each screen exposes a stateless version (`...Content`) used by previews and screenshot tests.
- **`:designsystem`** holds the theme (`ConversesTheme`, `PersonTheme`, colors, Roboto Flex) and the custom components; it is the only place with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
- **Protected people:** a cross-cutting concern. `PersonLockedScreen` only receives name, shape and color and loads nothing until `BiometricPrompt` succeeds; `FLAG_SECURE` (`SecureWindow`) is set on screens that show a protected person's data; the database and DataStore are excluded from Android backup. Any new feature (list, shortcuts, export, quick note) must not leak a protected person's data before unlocking.
- **Person color:** a person's screens (detail, editor, quick note with a person) take the color of their avatar via `PersonTheme`; the list, Archived and Settings use the app theme.
- **`EntryForm`** is the only entry form (quick note, new entry and edit). `whoLabel(...)` is the only function producing the labels Me / {first name} / Note; `EntryKind` (`SAID`, `HEARD`, `HAPPENED`) does not change.
- **Room:** version 1 (the first release; no migrations yet), schema in `app/schemas`. A schema change requires a migration and a test (`MigrationTestHelper`). In debug the database is recreated on schema changes; sample data is not seeded by default, only from the debug menu (`insertSampleData`).
- **Tests:** `FakeDb` (in-memory DAOs) and `MainDispatcherRule` in `app/src/test/.../testing`. Screenshot test method names use camelCase with no `_`; references are versioned in `<module>/src/screenshotTestDebug/reference/`.
- **CI** (`.github/workflows`): `pr-build.yml` (tests + debug APK), `build-apk.yml` and `screenshot-tests.yml`. The `update-screenshots` label on a PR regenerates the references; there is no need to generate them locally.

## Rules

- Work in small steps and validate each one before continuing; state and pending work live in `docs/plan.md`.
- Before adding a library, check its current stable version in the official documentation.
- Keep all `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` in the `designsystem` module; screens do not repeat them.
- UI text always goes in `strings.xml`: English by default (`values/`), with Catalan (`values-ca/`) and Spanish (`values-es/`).
- Gradle with a version catalog (`gradle/libs.versions.toml`).

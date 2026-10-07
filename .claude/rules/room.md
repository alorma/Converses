---
paths:
  - "app/src/main/java/com/alorma/contactnotes/data/db/**"
  - "app/schemas/**"
  - "app/src/androidTest/**"
  - "app/src/main/java/com/alorma/contactnotes/di/AppModules.kt"
---

# Room: the database is published

Converses v1 is live on Google Play (alpha track). Real users have a `converses.db` at schema version 1. **Every database change MUST ship with a migration and a migration test.** No exceptions, no "it's only alpha".

## What counts as a schema change

Anything that changes the exported schema JSON:

- adding, removing or renaming an entity (`@Entity`), a column, an index, a foreign key or a primary key;
- changing a column's type, nullability or default value;
- adding an entity to `@Database(entities = [...])` in `ConversesDatabase.kt`.

Enums (`ChannelType`, `EntryKind`, `AvatarShape`, `AvatarColor`) are stored by name. Adding a value is safe; **renaming or removing one breaks existing rows** and needs a data migration (`UPDATE ... SET type = 'NEW' WHERE type = 'OLD'`) even though the schema JSON does not change.

DAO-only changes (new queries) are not schema changes.

## Checklist for every schema change

1. Bump `version` in `ConversesDatabase.kt` (one step: 1 → 2, 2 → 3…).
2. Build (`./gradlew :app:assembleDebug`) so KSP exports `app/schemas/.../<new version>.json`, and commit it.
3. **Never edit or delete an exported schema JSON** (`1.json` and later). They are what users have on their devices.
4. Write the migration:
   - simple additive changes: `@AutoMigration(from = N, to = N + 1)` in `@Database(autoMigrations = [...])`, with an `AutoMigrationSpec` for renames/deletes (`@RenameColumn`, `@DeleteColumn`, `@RenameTable`, `@DeleteTable`);
   - anything else: a manual `Migration(N, N + 1)` in `data/db/` (e.g. `Migrations.kt`), registered with `.addMigrations(...)` in `databaseModule` (`di/AppModules.kt`).
   - New `NOT NULL` columns need a `DEFAULT` so existing rows stay valid.
5. Add a migration test in `app/src/androidTest/` with `MigrationTestHelper` (`room-testing` is already in `androidTestImplementation`; the Room Gradle plugin exposes `app/schemas` to the test assets): create the DB at version N, insert representative rows, run the migration to N + 1 and assert the data survived. Also run `runMigrationsAndValidate` from version 1 to the latest.
6. Update `docs/plan.md` («Regla de la base de dades» and «Model de dades», which states the current version).

## Never

- Rely on `fallbackToDestructiveMigration`: it is enabled only in debug (`isDebuggable` in `AppModules.kt`) and must never be enabled for release, since it wipes users' notes.
- Reset the database to a "clean version 1" or regenerate `1.json`; that was only possible before the release.
- Ask users to clear app data or reinstall.

Instrumented tests are not run by the CI (deliberate, see `CLAUDE.md`), so run the migration tests locally on an emulator (`./gradlew :app:connectedDebugAndroidTest`) and say so in the PR.

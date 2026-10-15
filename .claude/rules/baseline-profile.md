# Baseline Profile: check before a release

No `paths` on purpose: the check below applies when preparing a release, not when editing a file.

## When

When the release is being prepared (the same moment as the release notes), say whether the Baseline Profile
(`app/src/release/generated/baselineProfiles/`) needs regenerating, and why. Do it for every release, even if the answer is no.

## How to decide

1. Find the last commit that touched the profile: `git log -1 --format=%h -- app/src/release/generated/baselineProfiles`.
2. Look at what changed since then in the startup path and the main screens:
   `git diff --stat <hash>..HEAD -- app/src/main/java/com/alorma/contactnotes/{ConversesApp.kt,MainActivity.kt,ui/people,ui/person,ui/entry,ui/edit,ui/settings,ui/navigation,di}`
   plus new dependencies in `gradle/libs.versions.toml` (Compose, Koin, Room, navigation).
3. **Regenerate** if a main screen or the navigation was rewritten, a tab was added, or the startup changed (Koin modules,
   Room, initial composition). Also if the profile is more than two releases old.
4. **Do not regenerate** for small changes (texts, logic, secondary screens, bug fixes): stale entries are ignored and the
   rest keeps working.

## What to do

Propose it and ask. Do not generate without agreement. To generate: start an **emulator** (never Bernat's phone), then run
`ANDROID_SERIAL=<emulator> ./gradlew :app:generateReleaseBaselineProfile`. The generator starts from a clean install, skips the
onboarding and creates its own person and entry through the UI (the release app has no sample data), so the UI must be in
English. The AGP uninstalls the app when it ends. Check that `baseline-prof.txt` changed, has readable names (`Lcom/alorma/...`,
not `La02;`) and has entries for `ui/people`, `ui/person`, `ui/entry` and `ui/settings`, run `./gradlew ktlintCheck` and commit
the two generated files on their own. If the generator no longer matches the app (a new tab, labels renamed), say so before.

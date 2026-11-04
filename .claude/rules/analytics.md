---
paths:
  - "app/src/main/java/com/alorma/contactnotes/ui/**"
  - "app/src/main/java/com/alorma/contactnotes/data/**"
---

# Analytics: every feature decides what it tracks, and never leaks data

Usage statistics are opt-in and documented in `docs/analytics/README.md` (privacy rules, architecture, catalogs). This rule is the checklist for any new or changed feature.

## Decide, every time

When a task adds or changes a screen, a user action, something that happens on its own, or an error the app swallows, say in the proposal what it tracks (or why nothing) and add it in the same task, with a JVM test when it goes through a ViewModel.

## Where each thing goes

- **Screens:** add the route to `ui/tracking/Screens.kt` (`toScreen()`). Never a separate `TrackScreen` in the screen: there is one in `AppNavigation`. `PersonLockedRoute` is never tracked.
- **User actions:** `ui/tracking/Actions.kt`, called from the ViewModel (tested with a recording tracker); opening sheets and dialogs from the stateful screen with `rememberActionTracker()`. Sheets and dialogs are actions, not screens. Do not track what is already a screen.
- **Events** (things the user did not ask for): `data/tracking/Events.kt`.
- **Errors:** only those the app catches and swallows, with a new closed name in `data/tracking/ErrorSites.kt` for `where`. Crashes go to Crashlytics on their own. Never pass the exception message anywhere yourself.

## What may be sent

- Only closed values: enums, booleans, fixed names. **Never** text the user wrote, person names, ids, custom channel names, dates or counts that identify someone. A custom channel is only `custom_channel=true`.
- Nothing that reveals a protected person: opening one counts like any other person, and no event may say a person is protected.
- Changing the meaning or the properties of an existing event bumps its `eventVersion`.
- A new common property (like `is_pro`) goes through `EventTracker.setCommonProperty`, never into every event.

## When what leaves the phone changes

A new kind of data (a new common property, a new SDK, a new identifier) also needs the privacy policy and the Play data safety form reviewed: see `external-privacy.md`. Update `docs/analytics/README.md` (catalogs) in the same task.

---
name: plant-seed
description: Leave an emulator with the release app in a known state: onboarding skipped and a few fake people with entries, so the people list shows data. Use before anything that needs the app with data on a clean install (benchmarks, A/B startup measurements, screenshots, manual checks).
disable-model-invocation: true
---

# Plant seed

Puts the app on an **emulator** in a known state: release package installed, onboarding skipped, fake people and entries
created through the UI. The release build has no sample data and no import (`insertSampleData` only exists in debug), so it is
all done by hand with `adb`, text dumps of the screen and screenshots; no scripts.

## Rules

- **Emulator only.** The package is the RELEASE one (`com.alorma.contactnotes`, no `.dev`) and its data is wiped. Never run
  this on Bernat's phone. With two devices, use `adb -s emulator-XXXX` in every call (and `ANDROID_SERIAL=emulator-XXXX` for Gradle).
- The UI must be in **English**.
- Tap by **text or description** from a fresh dump, never by remembered coordinates (the ones below are only an example).
- Do **not** press Back on the people list (it leaves the app and later taps hit another app).
- The Baseline Profile generator (`:baselineprofile`) does NOT need this skill: it creates its own person. Use this one when
  you need the app to stay with data after the work (the AGP uninstalls the app when a generation ends).

## Looking at the screen

```bash
adb -s $S shell uiautomator dump /sdcard/u.xml >/dev/null
adb -s $S exec-out cat /sdcard/u.xml | tr '>' '\n' | grep -E ' text="[^"]+"| content-desc="[^"]+"' \
  | sed -E 's/.* text="([^"]*)".* content-desc="([^"]*)".* clickable="([^"]*)".* bounds="([^"]*)".*/\1 | \2 | \3 | \4/'
```

Each line is `text | description | clickable | [x1,y1][x2,y2]`. Tap the centre of the bounds with
`adb -s $S shell input tap X Y`. Write text with `adb -s $S shell input text "Marta%sSoler"` (`%s` is a space; `'` and `%`
need care, avoid them). Take a screenshot (`adb -s $S exec-out screencap -p > file.png`, then read it) at the end as evidence.

## Steps

1. **Device.** `adb devices`; pick the emulator as `$S`. Stop and ask if there is none. If there is none running,
   `emulator -list-avds` and start a phone one.
2. **Install and wipe.** `ANDROID_SERIAL=$S ./gradlew :app:installNonMinifiedRelease`, then
   `adb -s $S shell pm clear com.alorma.contactnotes`.
3. **Launch.** `adb -s $S shell monkey -p com.alorma.contactnotes -c android.intent.category.LAUNCHER 1`.
4. **Onboarding.** Tap «Skip» (top right). The people list shows «No people yet» with «Add a person» as description.
5. **For each person in the table below:**
   1. Tap «Add a person» (description, top right). The editor shows «New person».
   2. Tap the Name field and type the name; tap the Alias field and type the alias.
   3. Tap «Save». It opens the person detail (name, `@alias`, «Timeline»).
   4. For each entry: tap «New entry: Me» (or «New entry: {first name}» / «New entry: Note») in the empty timeline, or the «Note»
      button at the bottom once it has entries. In the form pick the «Where» chip, tap the text field, type the text and tap
      «Save to timeline». The detail then shows «N entry/entries».
   5. Tap «Back» (description) to return to the list.
6. **Check.** The list shows every person with their last entry, and does not show «No people yet». Take a screenshot.

## Seed

| Name | Alias | Entries (who, where, text) |
| --- | --- | --- |
| Marta Soler | Marta | Me, Chat: «I told her I cannot make it to dinner on Saturday.»; her, In person: «She said she moved to a new flat.» |
| Jordi Puig | Jordi | her/him, Phone: «He wants to set up a game on Friday.» |
| Laia Ferrer | (none) | Note, Other: «Ask about the flat keys.» |

Only the first person and its first entry have been run end to end with this exact flow; the rest repeat the same steps.

## Report

Say the device used and show the screenshot of the people list with data. If a step failed, say which one and why.

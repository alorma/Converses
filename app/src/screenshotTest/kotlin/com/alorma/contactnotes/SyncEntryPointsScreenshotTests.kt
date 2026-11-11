package com.alorma.contactnotes

import androidx.compose.runtime.Composable
import com.alorma.contactnotes.ui.navigation.AppShellRailAccountPreview
import com.alorma.contactnotes.ui.navigation.AppShellRailAccountSelectedPreview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep3SyncPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenSyncPreview
import com.alorma.contactnotes.ui.settings.SettingsSyncKeyPreview
import com.alorma.contactnotes.ui.settings.SettingsSyncOffPreview
import com.alorma.contactnotes.ui.settings.SettingsSyncOnPreview
import com.alorma.contactnotes.ui.sync.SyncIndicatorStatesPreview
import com.android.tools.screenshot.PreviewTest

/** Els punts d'entrada de la sincronització (13a, 13b, 13r, 0c), que només es veuen amb SyncFlag encès. */
class SyncEntryPointsScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun syncIndicatorStates() {
    SyncIndicatorStatesPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun peopleListSyncIndicator() {
    PeopleListScreenSyncPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsSyncOff() {
    SettingsSyncOffPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsSyncOn() {
    SettingsSyncOnPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsSyncKey() {
    SettingsSyncKeyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun onboardingStep3Sync() {
    OnboardingStep3SyncPreview()
  }

  @TabletLandscapePreviews
  @PreviewTest
  @Composable
  fun appShellRailAccount() {
    AppShellRailAccountPreview()
  }

  @TabletLandscapePreviews
  @PreviewTest
  @Composable
  fun appShellRailAccountSelected() {
    AppShellRailAccountSelectedPreview()
  }
}

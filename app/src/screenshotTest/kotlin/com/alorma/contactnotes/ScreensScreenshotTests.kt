package com.alorma.contactnotes

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.ui.consent.ConsentSheetUnchosenPreview
import com.alorma.contactnotes.ui.consent.ConsentSheetYesPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetAutoPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetCustomPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetDowngradePreview
import com.alorma.contactnotes.ui.edit.AvatarSheetLockedPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetTrialPreview
import com.alorma.contactnotes.ui.edit.DeletePersonDialogPreview
import com.alorma.contactnotes.ui.edit.EditPersonCustomAvatarPreview
import com.alorma.contactnotes.ui.edit.EditPersonExistingPreview
import com.alorma.contactnotes.ui.edit.EditPersonLockedAvatarPreview
import com.alorma.contactnotes.ui.edit.EditPersonNewPreview
import com.alorma.contactnotes.ui.edit.EditPersonProtectedPreview
import com.alorma.contactnotes.ui.entry.CustomChannelSheetNewPreview
import com.alorma.contactnotes.ui.entry.CustomChannelSheetTakenPreview
import com.alorma.contactnotes.ui.entry.EditEntrySheetPreview
import com.alorma.contactnotes.ui.entry.NewEntrySheetPreview
import com.alorma.contactnotes.ui.entry.PersonPickerFewPreview
import com.alorma.contactnotes.ui.entry.PersonPickerManyPreview
import com.alorma.contactnotes.ui.entry.QuickNoteCustomChannelsLockedPreview
import com.alorma.contactnotes.ui.entry.QuickNoteCustomChannelsPreview
import com.alorma.contactnotes.ui.entry.QuickNoteFilledPreview
import com.alorma.contactnotes.ui.entry.QuickNoteNoPersonPreview
import com.alorma.contactnotes.ui.entry.QuickNoteSharedNoPeoplePreview
import com.alorma.contactnotes.ui.entry.QuickNoteSharedPreview
import com.alorma.contactnotes.ui.entry.WhenSheetCustomPreview
import com.alorma.contactnotes.ui.entry.WhenSheetNowPreview
import com.alorma.contactnotes.ui.lock.AppLockPreview
import com.alorma.contactnotes.ui.navigation.AppShellArchivedPreview
import com.alorma.contactnotes.ui.navigation.AppShellBookPreview
import com.alorma.contactnotes.ui.navigation.AppShellPeoplePreview
import com.alorma.contactnotes.ui.navigation.AppShellRailPeoplePreview
import com.alorma.contactnotes.ui.navigation.AppShellRailSettingsPreview
import com.alorma.contactnotes.ui.navigation.AppShellTwoPaneEmptyPreview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep1Preview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep2Preview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep3Preview
import com.alorma.contactnotes.ui.paywall.PaywallDevicesPreview
import com.alorma.contactnotes.ui.paywall.PaywallFromChannelsPreview
import com.alorma.contactnotes.ui.paywall.PaywallOfflinePreview
import com.alorma.contactnotes.ui.paywall.PaywallOwnedPreview
import com.alorma.contactnotes.ui.paywall.PaywallPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenArchivedPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenEmptyPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenLoadedPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenTasksPreview
import com.alorma.contactnotes.ui.person.AvatarColorProvider
import com.alorma.contactnotes.ui.person.ConfirmDeleteDialogPreview
import com.alorma.contactnotes.ui.person.EntryActionsDayPreview
import com.alorma.contactnotes.ui.person.EntryActionsSinglePreview
import com.alorma.contactnotes.ui.person.MoveEntryDialogPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenCustomChannelPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenEmptyPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenFilteredEmptyPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenLoadedPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenSingleFactPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenThemedPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenTwoColumnsPreview
import com.alorma.contactnotes.ui.person.PersonDetailTasksEmptyPreview
import com.alorma.contactnotes.ui.person.PersonDetailTasksLockedPreview
import com.alorma.contactnotes.ui.person.PersonDetailTasksPreview
import com.alorma.contactnotes.ui.person.PersonLockedCanceledPreview
import com.alorma.contactnotes.ui.person.PersonLockedLockedOutPreview
import com.alorma.contactnotes.ui.person.PersonLockedNoCredentialsPreview
import com.alorma.contactnotes.ui.person.PersonLockedWaitingPreview
import com.alorma.contactnotes.ui.person.TasksSectionDonePreview
import com.alorma.contactnotes.ui.settings.CustomChannelsEmptyPreview
import com.alorma.contactnotes.ui.settings.CustomChannelsPreview
import com.alorma.contactnotes.ui.settings.SettingsAnalyticsOnPreview
import com.alorma.contactnotes.ui.settings.SettingsAppLockPreview
import com.alorma.contactnotes.ui.settings.SettingsNoLockPreview
import com.alorma.contactnotes.ui.settings.SettingsPersonColorLockedPreview
import com.alorma.contactnotes.ui.settings.SettingsProPreview
import com.alorma.contactnotes.ui.task.TaskFormEditPreview
import com.alorma.contactnotes.ui.task.TaskFormEmptyPreview
import com.alorma.contactnotes.ui.task.TaskFormFilledPreview
import com.android.tools.screenshot.PreviewTest

/** Pantalla d'un mòbil de mida mitjana, en clar i en fosc, amb l'idioma fixat perquè no depengui de la màquina. */
@Preview(name = "Light", locale = "en", widthDp = 411, heightDp = 891)
@Preview(
  name = "Dark",
  locale = "en",
  widthDp = 411,
  heightDp = 891,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ScreenPreviews

/** Pantalla allargada, per a seccions que queden per sota del primer cop d'ull (Ajustos › Privacitat). */
@Preview(name = "Light", locale = "en", widthDp = 411, heightDp = 2000)
@Preview(
  name = "Dark",
  locale = "en",
  widthDp = 411,
  heightDp = 2000,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class TallScreenPreviews

/** Font gran, per veure que els textos no es tallen. Només en clar i en anglès. */
@Preview(name = "FontScale2", locale = "en", widthDp = 411, heightDp = 891, fontScale = 2f)
annotation class LargeFontScreenPreviews

/**
 * Tauleta vertical (amplada mitjana), en clar i en fosc. A 160 dpi (1 dp = 1 px): amb la densitat per defecte la
 * captura fa 2100 × 3360 px i la validació es queda sense memòria.
 */
@Preview(name = "Light", locale = "en", device = "spec:width=800dp,height=1280dp,dpi=160")
@Preview(
  name = "Dark",
  locale = "en",
  device = "spec:width=800dp,height=1280dp,dpi=160",
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class TabletPortraitPreviews

/** Tauleta horitzontal (amplada expandida), en clar i en fosc; a 160 dpi com [TabletPortraitPreviews]. */
@Preview(name = "Light", locale = "en", device = "spec:width=1280dp,height=800dp,dpi=160")
@Preview(
  name = "Dark",
  locale = "en",
  device = "spec:width=1280dp,height=800dp,dpi=160",
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class TabletLandscapePreviews

/** Panell de detall d'una tauleta horitzontal (el que queda al costat del rail i del llistat), a 160 dpi. */
@Preview(name = "Light", locale = "en", device = "spec:width=764dp,height=768dp,dpi=160")
@Preview(
  name = "Dark",
  locale = "en",
  device = "spec:width=764dp,height=768dp,dpi=160",
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class DetailPanePreviews

/** Plegable obert (la pantalla interior d'un Pixel 9 Pro Fold), a 160 dpi. */
@Preview(name = "Light", locale = "en", device = "spec:width=852dp,height=883dp,dpi=160")
@Preview(
  name = "Dark",
  locale = "en",
  device = "spec:width=852dp,height=883dp,dpi=160",
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class FoldablePreviews

class ScreensScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun appShellPeople() {
    AppShellPeoplePreview()
  }

  @TabletPortraitPreviews
  @PreviewTest
  @Composable
  fun appShellRailPeople() {
    AppShellRailPeoplePreview()
  }

  @TabletLandscapePreviews
  @PreviewTest
  @Composable
  fun appShellTwoPaneEmpty() {
    AppShellTwoPaneEmptyPreview()
  }

  @FoldablePreviews
  @PreviewTest
  @Composable
  fun appShellBook() {
    AppShellBookPreview()
  }

  @DetailPanePreviews
  @PreviewTest
  @Composable
  fun personDetailTwoColumns() {
    PersonDetailScreenTwoColumnsPreview()
  }

  @TabletLandscapePreviews
  @PreviewTest
  @Composable
  fun appShellRailSettings() {
    AppShellRailSettingsPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun appShellArchived() {
    AppShellArchivedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun peopleListLoaded() {
    PeopleListScreenLoadedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun peopleListEmpty() {
    PeopleListScreenEmptyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun peopleListArchived() {
    PeopleListScreenArchivedPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailLoaded() {
    PersonDetailScreenLoadedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailCustomChannel() {
    PersonDetailScreenCustomChannelPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailSingleFact() {
    PersonDetailScreenSingleFactPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailEmpty() {
    PersonDetailScreenEmptyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailThemed(
    @PreviewParameter(AvatarColorProvider::class) color: AvatarColor,
  ) {
    PersonDetailScreenThemedPreview(color)
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personLockedWaiting() {
    PersonLockedWaitingPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun personLockedCanceled() {
    PersonLockedCanceledPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personLockedLockedOut() {
    PersonLockedLockedOutPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personLockedNoCredentials() {
    PersonLockedNoCredentialsPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteNoPerson() {
    QuickNoteNoPersonPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteFilled() {
    QuickNoteFilledPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteShared() {
    QuickNoteSharedPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteCustomChannels() {
    QuickNoteCustomChannelsPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteCustomChannelsLocked() {
    QuickNoteCustomChannelsLockedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteSharedNoPeople() {
    QuickNoteSharedNoPeoplePreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun newEntrySheet() {
    NewEntrySheetPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun consentSheetUnchosen() {
    ConsentSheetUnchosenPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun consentSheetYes() {
    ConsentSheetYesPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun customChannelSheetNew() {
    CustomChannelSheetNewPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun customChannelSheetTaken() {
    CustomChannelSheetTakenPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun whenSheetNow() {
    WhenSheetNowPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun whenSheetCustom() {
    WhenSheetCustomPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun appLock() {
    AppLockPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonCustomAvatar() {
    EditPersonCustomAvatarPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonLockedAvatar() {
    EditPersonLockedAvatarPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun avatarSheetAuto() {
    AvatarSheetAutoPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun avatarSheetCustom() {
    AvatarSheetCustomPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun avatarSheetLocked() {
    AvatarSheetLockedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun avatarSheetTrial() {
    AvatarSheetTrialPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun avatarSheetDowngrade() {
    AvatarSheetDowngradePreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personPickerFew() {
    PersonPickerFewPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personPickerMany() {
    PersonPickerManyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun entryActionsSingle() {
    EntryActionsSinglePreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun entryActionsDay() {
    EntryActionsDayPreview()
  }

  @TabletLandscapePreviews
  @PreviewTest
  @Composable
  fun onboardingStep1TabletLandscape() {
    OnboardingStep1Preview()
  }

  @TabletPortraitPreviews
  @PreviewTest
  @Composable
  fun onboardingStep2TabletPortrait() {
    OnboardingStep2Preview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun onboardingStep1() {
    OnboardingStep1Preview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun onboardingStep2() {
    OnboardingStep2Preview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun onboardingStep3() {
    OnboardingStep3Preview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun paywall() {
    PaywallPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun paywallFromChannels() {
    PaywallFromChannelsPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun paywallOffline() {
    PaywallOfflinePreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun paywallOwned() {
    PaywallOwnedPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun paywallDevices() {
    PaywallDevicesPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun customChannels() {
    CustomChannelsPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun customChannelsEmpty() {
    CustomChannelsEmptyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsPro() {
    SettingsProPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun settingsAppLock() {
    SettingsAppLockPreview()
  }

  @TallScreenPreviews
  @PreviewTest
  @Composable
  fun settingsAnalyticsOn() {
    SettingsAnalyticsOnPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsNoLock() {
    SettingsNoLockPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsPersonColorLocked() {
    SettingsPersonColorLockedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonNew() {
    EditPersonNewPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonExisting() {
    EditPersonExistingPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonProtected() {
    EditPersonProtectedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun confirmDeleteDialog() {
    ConfirmDeleteDialogPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun moveEntryDialog() {
    MoveEntryDialogPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun deletePersonDialog() {
    DeletePersonDialogPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailFilteredEmpty() {
    PersonDetailScreenFilteredEmptyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editEntrySheet() {
    EditEntrySheetPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun taskFormEmpty() {
    TaskFormEmptyPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun taskFormFilled() {
    TaskFormFilledPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun taskFormEdit() {
    TaskFormEditPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailTasks() {
    PersonDetailTasksPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailTasksEmpty() {
    PersonDetailTasksEmptyPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailTasksLocked() {
    PersonDetailTasksLockedPreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun tasksSectionDone() {
    TasksSectionDonePreview()
  }

  @ScreenPreviews
  @LargeFontScreenPreviews
  @PreviewTest
  @Composable
  fun peopleListTasks() {
    PeopleListScreenTasksPreview()
  }
}

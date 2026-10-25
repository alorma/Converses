package com.alorma.contactnotes

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.ui.approval.ApprovalConfirmPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetAutoPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetCustomPreview
import com.alorma.contactnotes.ui.edit.ContactsExplainSheetPreview
import com.alorma.contactnotes.ui.edit.DeletePersonDialogPreview
import com.alorma.contactnotes.ui.edit.EditPersonContactLinkedPreview
import com.alorma.contactnotes.ui.edit.EditPersonContactNoPermissionPreview
import com.alorma.contactnotes.ui.edit.EditPersonContactNotLinkedPreview
import com.alorma.contactnotes.ui.edit.EditPersonContactTakenPreview
import com.alorma.contactnotes.ui.edit.EditPersonCustomAvatarPreview
import com.alorma.contactnotes.ui.edit.EditPersonExistingPreview
import com.alorma.contactnotes.ui.edit.EditPersonNewPreview
import com.alorma.contactnotes.ui.edit.EditPersonProtectedPreview
import com.alorma.contactnotes.ui.entry.EditEntrySheetPreview
import com.alorma.contactnotes.ui.entry.NewEntrySheetPreview
import com.alorma.contactnotes.ui.entry.PersonPickerFewPreview
import com.alorma.contactnotes.ui.entry.PersonPickerManyPreview
import com.alorma.contactnotes.ui.entry.QuickNoteFilledPreview
import com.alorma.contactnotes.ui.entry.QuickNoteNoPersonPreview
import com.alorma.contactnotes.ui.entry.QuickNoteSharedNoPeoplePreview
import com.alorma.contactnotes.ui.entry.QuickNoteSharedPreview
import com.alorma.contactnotes.ui.entry.WhenSheetCustomPreview
import com.alorma.contactnotes.ui.entry.WhenSheetNowPreview
import com.alorma.contactnotes.ui.lock.AppLockPreview
import com.alorma.contactnotes.ui.navigation.AppShellArchivedPreview
import com.alorma.contactnotes.ui.navigation.AppShellPeoplePreview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep1Preview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep2Preview
import com.alorma.contactnotes.ui.onboarding.OnboardingStep3Preview
import com.alorma.contactnotes.ui.people.PeopleListScreenArchivedPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenEmptyPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenLoadedPreview
import com.alorma.contactnotes.ui.person.AvatarColorProvider
import com.alorma.contactnotes.ui.person.ConfirmDeleteDialogPreview
import com.alorma.contactnotes.ui.person.EntryActionsDayPreview
import com.alorma.contactnotes.ui.person.EntryActionsSinglePreview
import com.alorma.contactnotes.ui.person.MoveEntryDialogPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenContactDeletedPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenContactLinkedPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenEmptyPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenFilteredEmptyPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenLoadedPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenSingleFactPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenThemedPreview
import com.alorma.contactnotes.ui.person.PersonLockedCanceledPreview
import com.alorma.contactnotes.ui.person.PersonLockedLockedOutPreview
import com.alorma.contactnotes.ui.person.PersonLockedNoCredentialsPreview
import com.alorma.contactnotes.ui.person.PersonLockedWaitingPreview
import com.alorma.contactnotes.ui.settings.SettingsAppLockPreview
import com.alorma.contactnotes.ui.settings.SettingsContactsDeniedPreview
import com.alorma.contactnotes.ui.settings.SettingsContactsNeverOnPreview
import com.alorma.contactnotes.ui.settings.SettingsContactsOnPreview
import com.alorma.contactnotes.ui.settings.SettingsNoLockPreview
import com.alorma.contactnotes.ui.watchinstall.WatchInstallFinishPreview
import com.alorma.contactnotes.ui.watchinstall.WatchInstallProposePreview
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

/** Com [ScreenPreviews] però més alta: la secció Contactes d'Ajustos queda sota el plec d'una pantalla normal. */
@Preview(name = "Light", locale = "en", widthDp = 411, heightDp = 1500)
@Preview(
  name = "Dark",
  locale = "en",
  widthDp = 411,
  heightDp = 1500,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class TallScreenPreviews

/** Idiomes que no són l'anglès i font gran, per veure que els textos no es tallen. Només en clar. */
@Preview(name = "Catalan", locale = "ca", widthDp = 411, heightDp = 891)
@Preview(name = "Spanish", locale = "es", widthDp = 411, heightDp = 891)
@Preview(name = "FontScale2", locale = "en", widthDp = 411, heightDp = 891, fontScale = 2f)
annotation class LocalizedScreenPreviews

class ScreensScreenshotTests {
  @ScreenPreviews
  @PreviewTest
  @Composable
  fun appShellPeople() {
    AppShellPeoplePreview()
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
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailLoaded() {
    PersonDetailScreenLoadedPreview()
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
  fun personDetailContactLinked() {
    PersonDetailScreenContactLinkedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personDetailContactDeleted() {
    PersonDetailScreenContactDeletedPreview()
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
  @LocalizedScreenPreviews
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
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteFilled() {
    QuickNoteFilledPreview()
  }

  @ScreenPreviews
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteShared() {
    QuickNoteSharedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun quickNoteSharedNoPeople() {
    QuickNoteSharedNoPeoplePreview()
  }

  @ScreenPreviews
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun newEntrySheet() {
    NewEntrySheetPreview()
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
  fun editPersonContactNotLinked() {
    EditPersonContactNotLinkedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonContactLinked() {
    EditPersonContactLinkedPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonContactTaken() {
    EditPersonContactTakenPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun editPersonContactNoPermission() {
    EditPersonContactNoPermissionPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun contactsExplainSheet() {
    ContactsExplainSheetPreview()
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
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun settingsAppLock() {
    SettingsAppLockPreview()
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun settingsNoLock() {
    SettingsNoLockPreview()
  }

  @TallScreenPreviews
  @PreviewTest
  @Composable
  fun settingsContactsNeverOn() {
    SettingsContactsNeverOnPreview()
  }

  @TallScreenPreviews
  @PreviewTest
  @Composable
  fun settingsContactsOn() {
    SettingsContactsOnPreview()
  }

  @TallScreenPreviews
  @PreviewTest
  @Composable
  fun settingsContactsDenied() {
    SettingsContactsDeniedPreview()
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
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun approvalConfirm() {
    ApprovalConfirmPreview()
  }

  @ScreenPreviews
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun watchInstallPropose() {
    WatchInstallProposePreview()
  }

  @ScreenPreviews
  @LocalizedScreenPreviews
  @PreviewTest
  @Composable
  fun watchInstallFinish() {
    WatchInstallFinishPreview()
  }
}

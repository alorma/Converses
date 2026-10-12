package com.alorma.contactnotes

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.ui.approval.ApprovalConfirmPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetAutoPreview
import com.alorma.contactnotes.ui.edit.AvatarSheetCustomPreview
import com.alorma.contactnotes.ui.edit.DeletePersonDialogPreview
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
import com.alorma.contactnotes.ui.entry.WhenSheetCustomPreview
import com.alorma.contactnotes.ui.entry.WhenSheetNowPreview
import com.alorma.contactnotes.ui.lock.AppLockPreview
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
import com.alorma.contactnotes.ui.settings.SettingsNoLockPreview
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

/** Idiomes que no són l'anglès i font gran, per veure que els textos no es tallen. Només en clar. */
@Preview(name = "Catalan", locale = "ca", widthDp = 411, heightDp = 891)
@Preview(name = "Spanish", locale = "es", widthDp = 411, heightDp = 891)
@Preview(name = "FontScale2", locale = "en", widthDp = 411, heightDp = 891, fontScale = 2f)
annotation class LocalizedScreenPreviews

class ScreensScreenshotTests {
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
}

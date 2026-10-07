package com.alorma.contactnotes

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.alorma.contactnotes.ui.people.PeopleListScreenArchivedPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenEmptyPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenLoadedPreview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.ui.person.AvatarColorProvider
import com.alorma.contactnotes.ui.person.PersonDetailScreenEmptyPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenThemedPreview
import com.alorma.contactnotes.ui.person.PersonDetailScreenLoadedPreview
import com.alorma.contactnotes.ui.person.PersonLockedCanceledPreview
import com.alorma.contactnotes.ui.person.PersonLockedLockedOutPreview
import com.alorma.contactnotes.ui.person.PersonLockedNoCredentialsPreview
import com.alorma.contactnotes.ui.person.PersonLockedWaitingPreview

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
  @PreviewTest
  @Composable
  fun personDetailLoaded() {
    PersonDetailScreenLoadedPreview()
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
  fun personDetailThemed(@PreviewParameter(AvatarColorProvider::class) color: AvatarColor) {
    PersonDetailScreenThemedPreview(color)
  }

  @ScreenPreviews
  @PreviewTest
  @Composable
  fun personLockedWaiting() {
    PersonLockedWaitingPreview()
  }

  @ScreenPreviews
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
}

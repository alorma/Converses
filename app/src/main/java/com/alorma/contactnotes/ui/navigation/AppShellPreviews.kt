package com.alorma.contactnotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.people.PeopleListScreenLoadedPreview

@Preview
@Composable
internal fun AppShellPeoplePreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      showNavigation = true,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
    ) {
      PeopleListScreenLoadedPreview()
    }
  }
}

@Preview
@Composable
internal fun AppShellArchivedPreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = ArchivedRoute,
      showNavigation = true,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
    ) {
      PeopleListScreenLoadedPreview()
    }
  }
}

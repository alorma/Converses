package com.alorma.contactnotes.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.ui.people.PeopleListScreenLoadedPreview
import com.alorma.contactnotes.ui.people.PeopleListScreenPanePreview
import com.alorma.contactnotes.ui.settings.SettingsProPreview
import com.alorma.contactnotes.ui.sync.SyncIndicatorState

@Preview
@Composable
internal fun AppShellPeoplePreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.FloatingBar,
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
      navigation = ShellNavigation.FloatingBar,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
    ) {
      PeopleListScreenLoadedPreview()
    }
  }
}

/** Tauleta vertical: el rail en lloc de la barra flotant. */
@Preview(widthDp = 800, heightDp = 1280)
@Composable
internal fun AppShellRailPeoplePreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.Rail,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
    ) {
      PeopleListScreenLoadedPreview()
    }
  }
}

/** Tauleta horitzontal sense cap persona oberta: rail, llistat de 380 dp i el panell de detall buit. */
@Preview(widthDp = 1280, heightDp = 800)
@Composable
internal fun AppShellTwoPaneEmptyPreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.Rail,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
    ) {
      Row {
        Box(modifier = Modifier.width(380.dp)) { PeopleListScreenPanePreview() }
        Spacer(Modifier.width(24.dp))
        EmptyDetailPane()
      }
    }
  }
}

/** Plegable obert (12d): sense rail, el llistat fins a la frontissa i la barra flotant dins del llistat. */
@Preview(widthDp = 852, heightDp = 883)
@Composable
internal fun AppShellBookPreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.FloatingBar,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
      floatingBarWidth = 402.dp,
    ) {
      Row {
        Box(modifier = Modifier.width(402.dp)) { PeopleListScreenPanePreview() }
        Spacer(Modifier.width(48.dp))
        EmptyDetailPane()
      }
    }
  }
}

/** Tauleta horitzontal a Ajustos: el rail el marca i el contingut queda en una columna centrada. */
@Preview(widthDp = 1280, heightDp = 800)
@Composable
internal fun AppShellRailSettingsPreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.Rail,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
      settingsSelected = true,
    ) {
      SettingsProPreview()
    }
  }
}

/** Amb SyncFlag encès (13r): el compte a baix de tot del rail. */
@Preview(widthDp = 1280, heightDp = 800)
@Composable
internal fun AppShellRailAccountPreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.Rail,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
      accountIndicator = SyncIndicatorState.On("B"),
    ) {
      Row {
        Box(modifier = Modifier.width(380.dp)) { PeopleListScreenPanePreview() }
        Spacer(Modifier.width(24.dp))
        EmptyDetailPane()
      }
    }
  }
}

/** Sincronització oberta: el compte queda marcat al rail amb l'indicador de 56 dp. */
@Preview(widthDp = 1280, heightDp = 800)
@Composable
internal fun AppShellRailAccountSelectedPreview() {
  ConversesPreviewTheme {
    AppShell(
      currentTab = PeopleRoute,
      navigation = ShellNavigation.Rail,
      onTabSelected = {},
      onQuickNote = {},
      onOpenSettings = {},
      accountIndicator = SyncIndicatorState.NeedsKey("B"),
      accountSelected = true,
    ) {
      SettingsProPreview()
    }
  }
}

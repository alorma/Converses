package com.alorma.contactnotes.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.ConversesFloatingToolbar
import com.alorma.contactnotes.designsystem.components.ConversesNavigationRail
import com.alorma.contactnotes.designsystem.components.RailDestination
import com.alorma.contactnotes.designsystem.components.ToolbarDestination

/**
 * Space a screen should leave at the end of its scrollable content so the last row can clear the
 * floating toolbar, which is drawn above the content. Constant on purpose: it must not change while a
 * screen is entering or leaving, or its list would jump when the toolbar shows or hides. With the rail
 * (large screens) there is no toolbar over the content, so it is zero.
 */
val LocalFloatingBarPadding = compositionLocalOf { PaddingValues() }

private val FloatingBarContentPadding = PaddingValues(bottom = 96.dp)
private val ScreenMargin = 16.dp

/**
 * Parent scaffold of the whole navigation; [navigation] picks how the main destinations are shown.
 *
 * - [ShellNavigation.FloatingBar] (phone): the toolbar lives in the FAB slot, so it is drawn over [content] (each
 *   screen keeps its own scaffold) instead of reserving space below it, and it stays still when switching tabs. It
 *   slides down and fades out when the screen on top is not a tab.
 * - [ShellNavigation.Rail] (600 dp and wider): a rail on the left that does reserve its space, with the quick note
 *   button on top instead of the FAB and Settings as one more destination.
 *
 * [content] is always called from the same place, whatever [navigation] is, so the screens keep their state when the
 * window is resized, rotated or folded.
 */
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppShell(
  currentTab: TopLevelRoute,
  navigation: ShellNavigation,
  onTabSelected: (TopLevelRoute) -> Unit,
  onQuickNote: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
  /** Settings (or a screen inside it) is open: the rail marks it instead of [currentTab]. */
  settingsSelected: Boolean = false,
  content: @Composable () -> Unit,
) {
  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      AnimatedVisibility(
        visible = navigation == ShellNavigation.FloatingBar,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
      ) {
        ConversesFloatingToolbar(
          fabIcon = Icons.Rounded.EditNote,
          fabContentDescription = stringResource(R.string.quick_note),
          onFabClick = onQuickNote,
          modifier = Modifier.padding(horizontal = ScreenMargin),
        ) {
          ToolbarDestination(
            icon = Icons.Rounded.Groups,
            contentDescription = stringResource(R.string.people_title),
            selected = currentTab == PeopleRoute,
            onClick = { onTabSelected(PeopleRoute) },
          )
          ToolbarDestination(
            icon = Icons.Rounded.Archive,
            contentDescription = stringResource(R.string.filter_archived),
            selected = currentTab == ArchivedRoute,
            onClick = { onTabSelected(ArchivedRoute) },
          )
          ToolbarDestination(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.settings_title),
            selected = false,
            onClick = onOpenSettings,
          )
        }
      }
    },
  ) {
    // The scaffold padding is ignored on purpose: the toolbar overlays the content.
    Row(modifier = Modifier.fillMaxSize()) {
      AnimatedVisibility(
        visible = navigation == ShellNavigation.Rail,
        enter = expandHorizontally() + fadeIn(),
        exit = shrinkHorizontally() + fadeOut(),
      ) {
        ConversesNavigationRail(
          fabIcon = Icons.Rounded.EditNote,
          fabContentDescription = stringResource(R.string.quick_note),
          onFabClick = onQuickNote,
        ) {
          RailDestination(
            icon = Icons.Outlined.Groups,
            selectedIcon = Icons.Rounded.Groups,
            label = stringResource(R.string.nav_people),
            selected = !settingsSelected && currentTab == PeopleRoute,
            onClick = { onTabSelected(PeopleRoute) },
          )
          RailDestination(
            icon = Icons.Outlined.Archive,
            selectedIcon = Icons.Rounded.Archive,
            label = stringResource(R.string.filter_archived),
            selected = !settingsSelected && currentTab == ArchivedRoute,
            onClick = { onTabSelected(ArchivedRoute) },
          )
          RailDestination(
            icon = Icons.Outlined.Settings,
            selectedIcon = Icons.Rounded.Settings,
            label = stringResource(R.string.settings_title),
            selected = settingsSelected,
            onClick = onOpenSettings,
          )
        }
      }
      Box(modifier = Modifier.weight(1f)) {
        val barPadding = if (navigation == ShellNavigation.Rail) PaddingValues() else FloatingBarContentPadding
        CompositionLocalProvider(LocalFloatingBarPadding provides barPadding) {
          content()
        }
      }
    }
  }
}

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.ConversesFloatingToolbar
import com.alorma.contactnotes.designsystem.components.ConversesNavigationRail
import com.alorma.contactnotes.designsystem.components.RailCustomDestination
import com.alorma.contactnotes.designsystem.components.RailDestination
import com.alorma.contactnotes.designsystem.components.ToolbarDestination
import com.alorma.contactnotes.ui.sync.SyncIndicatorState
import com.alorma.contactnotes.ui.sync.SyncIndicatorVisual
import com.alorma.contactnotes.ui.sync.descriptionRes

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
  /**
   * Open foldable: width of the list pane (up to the hinge). The toolbar and its FAB stay inside it, at the bottom,
   * instead of spanning the window across the hinge. `null` everywhere else.
   */
  floatingBarWidth: Dp? = null,
  /** Account indicator at the bottom of the rail; `null` (sync flag off) draws nothing there. */
  accountIndicator: SyncIndicatorState? = null,
  onOpenAccount: () -> Unit = {},
  /** Sync (or a screen inside it) is open: the rail marks the account instead of [currentTab]. */
  accountSelected: Boolean = false,
  content: @Composable () -> Unit,
) {
  val floatingBar: @Composable () -> Unit = {
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
  }
  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = { if (floatingBarWidth == null) floatingBar() },
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
          bottom =
            accountIndicator?.let { indicator ->
              {
                val description = stringResource(indicator.descriptionRes())
                RailCustomDestination(
                  label = stringResource(R.string.sync_rail_account),
                  selected = accountSelected,
                  onClick = onOpenAccount,
                  modifier = Modifier.semantics { contentDescription = description },
                ) {
                  SyncIndicatorVisual(state = indicator, background = MaterialTheme.colorScheme.surface)
                }
              }
            },
        ) {
          RailDestination(
            icon = Icons.Outlined.Groups,
            selectedIcon = Icons.Rounded.Groups,
            label = stringResource(R.string.nav_people),
            selected = !settingsSelected && !accountSelected && currentTab == PeopleRoute,
            onClick = { onTabSelected(PeopleRoute) },
          )
          RailDestination(
            icon = Icons.Outlined.Archive,
            selectedIcon = Icons.Rounded.Archive,
            label = stringResource(R.string.filter_archived),
            selected = !settingsSelected && !accountSelected && currentTab == ArchivedRoute,
            onClick = { onTabSelected(ArchivedRoute) },
          )
          RailDestination(
            icon = Icons.Outlined.Settings,
            selectedIcon = Icons.Rounded.Settings,
            label = stringResource(R.string.settings_title),
            selected = settingsSelected && !accountSelected,
            onClick = onOpenSettings,
          )
        }
      }
      Box(modifier = Modifier.weight(1f)) {
        val barPadding = if (navigation == ShellNavigation.Rail) PaddingValues() else FloatingBarContentPadding
        CompositionLocalProvider(LocalFloatingBarPadding provides barPadding) {
          content()
        }
        if (floatingBarWidth != null) {
          Box(
            modifier =
              Modifier
                .align(Alignment.BottomStart)
                .width(floatingBarWidth)
                .navigationBarsPadding()
                .padding(bottom = ScreenMargin),
          ) {
            floatingBar()
          }
        }
      }
    }
  }
}

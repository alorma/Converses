package com.alorma.contactnotes.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.ui.edit.EditPersonScreen
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteScreen
import com.alorma.contactnotes.ui.navigation.AppRoute
import com.alorma.contactnotes.ui.navigation.AppShell
import com.alorma.contactnotes.ui.navigation.ArchivedRoute
import com.alorma.contactnotes.ui.navigation.EditPersonRoute
import com.alorma.contactnotes.ui.navigation.OnboardingRoute
import com.alorma.contactnotes.ui.navigation.PaywallRoute
import com.alorma.contactnotes.ui.navigation.PeopleRoute
import com.alorma.contactnotes.ui.navigation.PersonLockedRoute
import com.alorma.contactnotes.ui.navigation.PersonRoute
import com.alorma.contactnotes.ui.navigation.QuickNoteRoute
import com.alorma.contactnotes.ui.navigation.SettingsRoute
import com.alorma.contactnotes.ui.navigation.TopLevelBackStack
import com.alorma.contactnotes.ui.navigation.TopLevelRoute
import com.alorma.contactnotes.ui.onboarding.OnboardingScreen
import com.alorma.contactnotes.ui.paywall.PaywallScreen
import com.alorma.contactnotes.ui.people.PeopleListRoute
import com.alorma.contactnotes.ui.person.PersonDetailRoute
import com.alorma.contactnotes.ui.person.PersonLockedScreen
import com.alorma.contactnotes.ui.settings.SettingsScreen
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
  startOnboarding: Boolean = false,
  /** Back stack built from a share / select-text intent; wins over [startOnboarding]. */
  initialBackStack: List<NavKey>? = null,
) {
  val backStack =
    rememberNavBackStack(
      *(initialBackStack ?: listOf(if (startOnboarding) OnboardingRoute else PeopleRoute)).toTypedArray(),
    )
  val imported = initialBackStack != null
  val navigation = remember(backStack) { TopLevelBackStack(backStack) }
  val activity = LocalActivity.current
  val selectionBridge: PersonSelectionBridge = koinInject()

  AppShell(
    currentTab = navigation.currentTab,
    showNavigation = navigation.current is TopLevelRoute,
    onTabSelected = navigation::switchTab,
    onQuickNote = { navigation.navigate(QuickNoteRoute()) },
    onOpenSettings = { navigation.navigate(SettingsRoute) },
  ) {
    NavDisplay(
      backStack = navigation.backStack,
      onBack = { if (!navigation.pop()) activity?.finish() },
      transitionSpec = { nativeTransition(forward = true, vertical = false) },
      popTransitionSpec = { nativeTransition(forward = false, vertical = false) },
      predictivePopTransitionSpec = { edge -> predictiveBack(edge, vertical = false) },
      entryDecorators =
        listOf(
          rememberSaveableStateHolderNavEntryDecorator(),
          rememberViewModelStoreNavEntryDecorator(),
        ),
      entryProvider =
        entryProvider {
          entry<OnboardingRoute> {
            OnboardingScreen(onFinished = { navigation.resetTo(PeopleRoute) })
          }
          entry<PeopleRoute> {
            PeopleListRoute(
              archived = false,
              onPersonClick = { person -> navigation.navigate(personRoute(person)) },
              onNewPerson = { navigation.navigate(EditPersonRoute()) },
            )
          }
          entry<ArchivedRoute>(metadata = tabTransitions) {
            PeopleListRoute(
              archived = true,
              onPersonClick = { person -> navigation.navigate(personRoute(person)) },
              onNewPerson = { navigation.navigate(EditPersonRoute()) },
            )
          }
          entry<QuickNoteRoute>(metadata = modalTransitions) { route ->
            QuickNoteScreen(
              personId = route.personId,
              sharedText = route.sharedText,
              // Entrat des d'una altra app: tancar torna a l'app d'origen
              onClose = { if (imported) activity?.finish() else navigation.pop() },
              onNewPerson = { navigation.navigate(EditPersonRoute(returnSelection = true)) },
            )
          }
          entry<PaywallRoute>(metadata = modalTransitions) { route ->
            PaywallScreen(highlight = route.highlight, onClose = { navigation.pop() })
          }
          entry<SettingsRoute> {
            SettingsScreen(onBack = { navigation.pop() })
          }
          entry<PersonLockedRoute> { route ->
            PersonLockedScreen(
              personId = route.id,
              onBack = { navigation.pop() },
              onUnlocked = {
                navigation.pop()
                navigation.navigate(PersonRoute(route.id, openNewEntry = route.openNewEntry))
              },
            )
          }
          entry<PersonRoute> { route ->
            PersonDetailRoute(
              personId = route.id,
              openNewEntry = route.openNewEntry,
              onBack = { navigation.pop() },
              onEdit = { focus -> navigation.navigate(EditPersonRoute(route.id, focus = focus)) },
            )
          }
          entry<EditPersonRoute>(metadata = modalTransitions) { route ->
            EditPersonScreen(
              personId = route.personId,
              focus = route.focus,
              onClose = { navigation.pop() },
              onSaved = { id ->
                navigation.pop()
                if (route.returnSelection) {
                  selectionBridge.post(id, authenticated = true)
                } else if (route.personId == 0L) {
                  navigation.navigate(PersonRoute(id))
                }
              },
              onDeleted = {
                navigation.pop()
                navigation.pop()
              },
            )
          }
        },
    )
  }
}

private fun personRoute(person: PersonEntity): AppRoute =
  if (person.isProtected) PersonLockedRoute(person.id) else PersonRoute(person.id)

private const val TransitionMs = 300
private const val FadeMs = 150
private const val BackScale = 0.9f

private val transitionEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Transitions close to the native Android ones: the new screen slides in a little; going back,
 * only the leaving screen shrinks and fades out, revealing the previous one (like predictive back).
 * Modal screens (quick note, editor) slide in vertically and leave the screen below untouched.
 */
private fun <S> AnimatedContentTransitionScope<S>.nativeTransition(
  forward: Boolean,
  vertical: Boolean,
): ContentTransform {
  val offset = tween<IntOffset>(TransitionMs, easing = transitionEasing)

  fun distance(size: Int) = size / 12
  val enter: EnterTransition
  val exit: ExitTransition
  if (forward) {
    // Opaque while it slides in: a fading entry lets the screen below show through it.
    enter = if (vertical) slideInVertically(offset, ::distance) else slideInHorizontally(offset, ::distance)
    exit = ExitTransition.None
  } else {
    enter = EnterTransition.None
    // Fully opaque until the very end, like the system animation: an early fade shows the screen
    // below through the leaving one.
    val lateFade = fadeOut(tween(TransitionMs / 3, delayMillis = TransitionMs * 2 / 3, easing = LinearEasing))
    exit =
      if (vertical) {
        slideOutVertically(offset, ::distance) + lateFade
      } else {
        scaleOut(tween(TransitionMs, easing = transitionEasing), targetScale = BackScale) + lateFade
      }
  }
  return enter togetherWith exit
}

/** Fade through: the leaving tab fades out quickly, then the new one fades in. */
private fun <S> AnimatedContentTransitionScope<S>.tabTransition(): ContentTransform =
  fadeIn(tween(TransitionMs - FadeMs / 2, delayMillis = FadeMs / 2, easing = LinearEasing)) togetherWith
    fadeOut(tween(FadeMs / 2, easing = LinearEasing))

private val modalTransitions =
  NavDisplay.transitionSpec { nativeTransition(forward = true, vertical = true) } +
    NavDisplay.popTransitionSpec { nativeTransition(forward = false, vertical = true) } +
    NavDisplay.predictivePopTransitionSpec { edge -> predictiveBack(edge, vertical = true) }

/**
 * Metadata for every tab but the base: going to it (`[People]` → `[People, Archived]`) fades. Leaving it
 * (back key, gesture or the toolbar) uses the default pop, where the tab shrinks and reveals People.
 * Adding a pop spec here makes the back key flicker.
 */
private val tabTransitions = NavDisplay.transitionSpec { tabTransition() }

private const val EdgeRight = 1
private const val GestureScale = 0.85f

/**
 * Back gesture (seeked by the finger): like the system animation, the leaving screen shrinks and
 * moves towards the side the gesture comes from, and only fades at the very end. The screen below
 * stays in place.
 */
private fun <S> AnimatedContentTransitionScope<S>.predictiveBack(
  edge: Int,
  vertical: Boolean,
): ContentTransform {
  val offset = tween<IntOffset>(TransitionMs, easing = LinearOutSlowInEasing)
  val side = if (edge == EdgeRight) -1 else 1
  val slide =
    if (vertical) {
      slideOutVertically(offset) { it / 8 }
    } else {
      slideOutHorizontally(offset) { side * it / 8 }
    }
  val exit =
    slide +
      scaleOut(tween(TransitionMs, easing = LinearOutSlowInEasing), targetScale = GestureScale) +
      fadeOut(tween(TransitionMs / 3, delayMillis = TransitionMs * 2 / 3, easing = LinearEasing))
  return EnterTransition.None togetherWith exit
}

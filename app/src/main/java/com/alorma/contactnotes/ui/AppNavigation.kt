package com.alorma.contactnotes.ui

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
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.alorma.contactnotes.ui.edit.EditFocus
import com.alorma.contactnotes.ui.edit.EditPersonScreen
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteScreen
import com.alorma.contactnotes.ui.onboarding.OnboardingScreen
import com.alorma.contactnotes.ui.people.PeopleListRoute
import com.alorma.contactnotes.ui.person.PersonDetailRoute
import com.alorma.contactnotes.ui.person.PersonLockedScreen
import com.alorma.contactnotes.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object OnboardingRoute : NavKey

@Serializable
data object PeopleRoute : NavKey

@Serializable
data object ArchivedRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class EditPersonRoute(
  val personId: Long = 0L,
  val returnSelection: Boolean = false,
  val focus: EditFocus? = null,
) : NavKey

@Serializable
data class QuickNoteRoute(val personId: Long = 0L) : NavKey

@Serializable
data class PersonRoute(val id: Long) : NavKey

@Serializable
data class PersonLockedRoute(val id: Long) : NavKey

@Composable
fun AppNavigation(startOnboarding: Boolean = false) {
  val backStack = rememberNavBackStack(if (startOnboarding) OnboardingRoute else PeopleRoute)
  val selectionBridge: PersonSelectionBridge = koinInject()

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    transitionSpec = { nativeTransition(forward = true, vertical = false) },
    popTransitionSpec = { nativeTransition(forward = false, vertical = false) },
    predictivePopTransitionSpec = { edge -> predictiveBack(edge, vertical = false) },
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<OnboardingRoute> {
        OnboardingScreen(
          onFinished = {
            backStack.clear()
            backStack.add(PeopleRoute)
          },
        )
      }
      entry<PeopleRoute> {
        PeopleListRoute(
          archived = false,
          onToggleArchivedList = { backStack.add(ArchivedRoute) },
          onPersonClick = { person ->
            backStack.add(if (person.isProtected) PersonLockedRoute(person.id) else PersonRoute(person.id))
          },
          onQuickNote = { backStack.add(QuickNoteRoute()) },
          onNewPerson = { backStack.add(EditPersonRoute()) },
          onOpenSettings = { backStack.add(SettingsRoute) },
        )
      }
      entry<ArchivedRoute> {
        PeopleListRoute(
          archived = true,
          onToggleArchivedList = { backStack.removeLastOrNull() },
          onPersonClick = { person ->
            backStack.add(if (person.isProtected) PersonLockedRoute(person.id) else PersonRoute(person.id))
          },
          onQuickNote = { backStack.add(QuickNoteRoute()) },
          onNewPerson = { backStack.add(EditPersonRoute()) },
          onOpenSettings = { backStack.add(SettingsRoute) },
        )
      }
      entry<QuickNoteRoute>(metadata = modalTransitions) { route ->
        QuickNoteScreen(
          personId = route.personId,
          onClose = { backStack.removeLastOrNull() },
          onNewPerson = { backStack.add(EditPersonRoute(returnSelection = true)) },
        )
      }
      entry<SettingsRoute> {
        SettingsScreen(onBack = { backStack.removeLastOrNull() })
      }
      entry<PersonLockedRoute> { route ->
        PersonLockedScreen(
          personId = route.id,
          onBack = { backStack.removeLastOrNull() },
          onUnlocked = {
            backStack.removeLastOrNull()
            backStack.add(PersonRoute(route.id))
          },
        )
      }
      entry<PersonRoute> { route ->
        PersonDetailRoute(
          personId = route.id,
          onBack = { backStack.removeLastOrNull() },
          onEdit = { focus -> backStack.add(EditPersonRoute(route.id, focus = focus)) },
        )
      }
      entry<EditPersonRoute>(metadata = modalTransitions) { route ->
        EditPersonScreen(
          personId = route.personId,
          focus = route.focus,
          onClose = { backStack.removeLastOrNull() },
          onSaved = { id ->
            backStack.removeLastOrNull()
            if (route.returnSelection) {
              selectionBridge.post(id)
            } else if (route.personId == 0L) {
              backStack.add(PersonRoute(id))
            }
          },
          onDeleted = {
            backStack.removeLastOrNull()
            backStack.removeLastOrNull()
          },
        )
      }
    },
  )
}

private const val TransitionMs = 300
private const val FadeMs = 150
private const val BackScale = 0.9f
private const val EnterStartAlpha = 0.6f
private const val ExitEndAlpha = 0.4f

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
    enter = (
      if (vertical) slideInVertically(offset, ::distance) else slideInHorizontally(offset, ::distance)
      ) + fadeIn(tween(TransitionMs, easing = LinearEasing), initialAlpha = EnterStartAlpha)
    exit = ExitTransition.None
  } else {
    enter = EnterTransition.None
    exit = if (vertical) {
      slideOutVertically(offset, ::distance) + fadeOut(tween(FadeMs + 100, easing = LinearEasing), targetAlpha = ExitEndAlpha)
    } else {
      scaleOut(tween(TransitionMs, easing = transitionEasing), targetScale = BackScale) +
        fadeOut(tween(FadeMs + 100, easing = LinearEasing), targetAlpha = ExitEndAlpha)
    }
  }
  return enter togetherWith exit
}

private val modalTransitions = NavDisplay.transitionSpec { nativeTransition(forward = true, vertical = true) } +
  NavDisplay.popTransitionSpec { nativeTransition(forward = false, vertical = true) } +
  NavDisplay.predictivePopTransitionSpec { edge -> predictiveBack(edge, vertical = true) }

private const val EdgeRight = 1
private const val GestureScale = 0.85f

/**
 * Back gesture (seeked by the finger): like the system animation, the leaving screen shrinks and
 * moves towards the side the gesture comes from, and only fades at the very end. The screen below
 * stays in place.
 */
private fun <S> AnimatedContentTransitionScope<S>.predictiveBack(edge: Int, vertical: Boolean): ContentTransform {
  val offset = tween<IntOffset>(TransitionMs, easing = LinearOutSlowInEasing)
  val side = if (edge == EdgeRight) -1 else 1
  val slide = if (vertical) {
    slideOutVertically(offset) { it / 8 }
  } else {
    slideOutHorizontally(offset) { side * it / 8 }
  }
  val exit = slide +
    scaleOut(tween(TransitionMs, easing = LinearOutSlowInEasing), targetScale = GestureScale) +
    fadeOut(tween(TransitionMs / 3, delayMillis = TransitionMs * 2 / 3, easing = LinearEasing))
  return EnterTransition.None togetherWith exit
}

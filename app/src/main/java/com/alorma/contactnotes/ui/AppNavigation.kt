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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.data.tracking.ConsentManager
import com.alorma.contactnotes.ui.consent.ConsentSheet
import com.alorma.contactnotes.ui.edit.EditPersonScreen
import com.alorma.contactnotes.ui.entry.PersonSelectionBridge
import com.alorma.contactnotes.ui.entry.QuickNoteScreen
import com.alorma.contactnotes.ui.navigation.AppRoute
import com.alorma.contactnotes.ui.navigation.AppShell
import com.alorma.contactnotes.ui.navigation.ArchivedRoute
import com.alorma.contactnotes.ui.navigation.CustomChannelsRoute
import com.alorma.contactnotes.ui.navigation.DetailPanel
import com.alorma.contactnotes.ui.navigation.EditPersonRoute
import com.alorma.contactnotes.ui.navigation.EmptyDetailPane
import com.alorma.contactnotes.ui.navigation.FormDialogFrame
import com.alorma.contactnotes.ui.navigation.FormDialogProperties
import com.alorma.contactnotes.ui.navigation.OnboardingRoute
import com.alorma.contactnotes.ui.navigation.PaywallRoute
import com.alorma.contactnotes.ui.navigation.PeopleRoute
import com.alorma.contactnotes.ui.navigation.PersonLockedRoute
import com.alorma.contactnotes.ui.navigation.PersonRoute
import com.alorma.contactnotes.ui.navigation.QuickNoteRoute
import com.alorma.contactnotes.ui.navigation.ScreenWidth
import com.alorma.contactnotes.ui.navigation.SettingsRoute
import com.alorma.contactnotes.ui.navigation.TaskRoute
import com.alorma.contactnotes.ui.navigation.TopLevelBackStack
import com.alorma.contactnotes.ui.navigation.currentScreenWidth
import com.alorma.contactnotes.ui.navigation.screenWidthOf
import com.alorma.contactnotes.ui.navigation.shellNavigationFor
import com.alorma.contactnotes.ui.onboarding.OnboardingScreen
import com.alorma.contactnotes.ui.paywall.PaywallScreen
import com.alorma.contactnotes.ui.paywall.ProBenefit
import com.alorma.contactnotes.ui.people.PeopleListRoute
import com.alorma.contactnotes.ui.person.PersonDetailRoute
import com.alorma.contactnotes.ui.person.PersonLockedScreen
import com.alorma.contactnotes.ui.settings.CustomChannelsScreen
import com.alorma.contactnotes.ui.settings.SettingsScreen
import com.alorma.contactnotes.ui.task.TaskFormScreen
import com.alorma.contactnotes.ui.tracking.TrackScreen
import com.alorma.contactnotes.ui.tracking.toScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation(
  startOnboarding: Boolean = false,
  /** Back stack built from a share / select-text intent; wins over [startOnboarding]. */
  initialBackStack: List<NavKey>? = null,
  /**
   * L'usuari encara no ha triat les estadístiques d'ús i no hi ha res a sobre (bloqueig de l'app). El full només
   * surt quan la pila és només People: mai a l'onboarding, ni sobre el paywall o una altra pantalla, ni en un import.
   */
  askConsent: Boolean = false,
) {
  val backStack =
    rememberNavBackStack(
      *(initialBackStack ?: listOf(if (startOnboarding) OnboardingRoute else PeopleRoute)).toTypedArray(),
    )
  val imported = initialBackStack != null
  val navigation = remember(backStack) { TopLevelBackStack(backStack) }
  val activity = LocalActivity.current
  val selectionBridge: PersonSelectionBridge = koinInject()

  // A partir de 600 dp, llistat i detall de costat. La directiva és l'única que ho decideix (també separa els panells
  // per la frontissa d'un plegable), i `twoPane` en surt perquè les pantalles i l'escena no es puguin contradir
  val windowInfo = currentWindowAdaptiveInfoV2()
  val density = LocalDensity.current
  // Plegable obert: la frontissa vertical, encara que sigui plana i no separi res (Pixel Fold). El llistat ocupa fins
  // a la frontissa i el detall, la resta; a cada costat queden `HingeMargin` sense res interactiu
  val hingeLeft =
    windowInfo.windowPosture.hingeList
      .firstOrNull { it.isVertical }
      ?.let { with(density) { it.bounds.left.toDp() } }
  val paneDirective =
    remember(windowInfo, hingeLeft) {
      val base = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowInfo)
      if (hingeLeft != null) {
        base.copy(
          horizontalPartitionSpacerSize = HingeMargin * 2,
          defaultPanePreferredWidth = hingeLeft - HingeMargin,
        )
      } else {
        val expanded = screenWidthOf(windowInfo.windowSizeClass.minWidthDp) == ScreenWidth.Expanded
        base.copy(defaultPanePreferredWidth = if (expanded) 380.dp else 300.dp)
      }
    }
  val twoPane = paneDirective.maxHorizontalPartitions > 1
  val book = twoPane && hingeLeft != null
  // PopLatest: enrere treu sempre una sola ruta, com `TopLevelBackStack.pop`. Amb el valor per defecte, tancar la
  // persona amb dos panells no compta com un canvi i enrere tancaria l'app amb la persona encara oberta
  val listDetailStrategy =
    rememberListDetailSceneStrategy<NavKey>(
      backNavigationBehavior = BackNavigationBehavior.PopLatest,
      directive = paneDirective,
    )
  // Amb dos panells, la nota ràpida, els formularis i Pro són diàlegs sobre el llistat i el detall. Un import des
  // d'una altra app no té res a sota: es queda a pantalla completa
  val formsAsDialogs = twoPane && !imported
  val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
  val secureWindow: SecureWindow = koinInject()
  val secure by secureWindow.isRequested.collectAsStateWithLifecycle(initialValue = false)
  val formMetadata =
    if (formsAsDialogs) DialogSceneStrategy.dialog(FormDialogProperties) else modalTransitions
  val openPerson: (PersonEntity) -> Unit = { person -> navigation.openPerson(personRoute(person)) }
  val newPerson = { navigation.navigate(EditPersonRoute()) }

  // Un sol lloc per a totes les pantalles: cada canvi de ruta (també tornant enrere) envia la seva
  val current = navigation.current
  key(current) { (current as? AppRoute)?.toScreen()?.let { TrackScreen(it) } }

  if (askConsent && !imported && navigation.backStack.singleOrNull() == PeopleRoute) {
    val consentManager: ConsentManager = koinInject()
    ConsentSheet(onChoice = consentManager::setConsent)
  }

  AppShell(
    currentTab = navigation.currentTab,
    navigation = shellNavigationFor(navigation.backStack, currentScreenWidth(), book = book),
    floatingBarWidth = if (book) hingeLeft?.minus(HingeMargin) else null,
    onTabSelected = navigation::switchTab,
    onQuickNote = {
      // Amb una persona oberta al costat, la nota ràpida ja ve amb ella triada (una protegida torna a demanar empremta)
      val openId = if (twoPane) (navigation.current as? PersonRoute)?.id else null
      navigation.navigate(QuickNoteRoute(personId = openId ?: 0L))
    },
    onOpenSettings = { navigation.navigateSingle(SettingsRoute) },
    settingsSelected = SettingsRoute in navigation.backStack,
  ) {
    NavDisplay(
      backStack = navigation.backStack,
      onBack = { if (!navigation.pop()) activity?.finish() },
      sceneStrategies = listOf(dialogStrategy, listDetailStrategy),
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
          entry<PeopleRoute>(
            metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { EmptyDetailPane() }),
          ) {
            PeopleListRoute(
              archived = false,
              onPersonClick = openPerson,
              onNewPerson = newPerson,
              inPane = twoPane,
              selectedPersonId = navigation.openPersonId,
            )
          }
          entry<ArchivedRoute>(
            metadata = tabTransitions + ListDetailSceneStrategy.listPane(detailPlaceholder = { EmptyDetailPane() }),
          ) {
            PeopleListRoute(
              archived = true,
              onPersonClick = openPerson,
              onNewPerson = newPerson,
              inPane = twoPane,
              selectedPersonId = navigation.openPersonId,
            )
          }
          entry<QuickNoteRoute>(metadata = formMetadata) { route ->
            FormDialogFrame(asDialog = formsAsDialogs, secure = secure) {
              QuickNoteScreen(
                personId = route.personId,
                sharedText = route.sharedText,
                // Entrat des d'una altra app: tancar torna a l'app d'origen
                onClose = { if (imported) activity?.finish() else navigation.pop() },
                onNewPerson = { navigation.navigate(EditPersonRoute(returnSelection = true)) },
                onOpenPro = { navigation.navigate(PaywallRoute(ProBenefit.Channels)) },
              )
            }
          }
          entry<TaskRoute>(metadata = formMetadata) { route ->
            FormDialogFrame(asDialog = formsAsDialogs, secure = secure) {
              TaskFormScreen(
                personId = route.personId,
                taskId = route.taskId,
                onClose = { navigation.pop() },
                onNewPerson = { navigation.navigate(EditPersonRoute(returnSelection = true)) },
              )
            }
          }
          entry<PaywallRoute>(metadata = formMetadata) { route ->
            FormDialogFrame(asDialog = formsAsDialogs, secure = secure) {
              PaywallScreen(highlight = route.highlight, onClose = { navigation.pop() })
            }
          }
          entry<SettingsRoute> {
            SettingsScreen(
              onBack = { navigation.pop() },
              onOpenPro = { navigation.navigate(PaywallRoute(it)) },
              onOpenCustomChannels = { navigation.navigate(CustomChannelsRoute) },
            )
          }
          entry<CustomChannelsRoute> {
            CustomChannelsScreen(onBack = { navigation.pop() })
          }
          entry<PersonLockedRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
            DetailPanel(inPane = twoPane) {
              PersonLockedScreen(
                personId = route.id,
                onBack = { navigation.pop() },
                onUnlocked = {
                  navigation.pop()
                  navigation.navigate(PersonRoute(route.id, openNewEntry = route.openNewEntry))
                },
                inPane = twoPane,
              )
            }
          }
          entry<PersonRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
            DetailPanel(inPane = twoPane) {
              PersonDetailRoute(
                personId = route.id,
                openNewEntry = route.openNewEntry,
                onBack = { navigation.pop() },
                onEdit = { focus -> navigation.navigate(EditPersonRoute(route.id, focus = focus)) },
                onOpenPro = { navigation.navigate(PaywallRoute(it)) },
                onNewTask = { navigation.navigate(TaskRoute(route.id)) },
                onOpenTask = { navigation.navigate(TaskRoute(route.id, it.id)) },
                inPane = twoPane,
              )
            }
          }
          entry<EditPersonRoute>(metadata = formMetadata) { route ->
            FormDialogFrame(asDialog = formsAsDialogs, secure = secure) {
              EditPersonScreen(
                personId = route.personId,
                focus = route.focus,
                onOpenPro = { navigation.navigate(PaywallRoute(it)) },
                onClose = { navigation.pop() },
                onSaved = { id ->
                  navigation.pop()
                  if (route.returnSelection) {
                    selectionBridge.post(id, authenticated = true)
                  } else if (route.personId == 0L) {
                    // Amb dos panells pot haver-hi una altra persona oberta: la nova la substitueix
                    navigation.openPerson(PersonRoute(id))
                  }
                },
                onDeleted = {
                  navigation.pop()
                  navigation.pop()
                },
              )
            }
          }
        },
    )
  }
}

/** Espai sense res interactiu a cada costat de la frontissa d'un plegable obert. */
private val HingeMargin = 24.dp

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

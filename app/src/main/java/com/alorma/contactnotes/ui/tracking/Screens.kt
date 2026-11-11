package com.alorma.contactnotes.ui.tracking

import com.alorma.contactnotes.data.tracking.Screen
import com.alorma.contactnotes.ui.navigation.AppRoute
import com.alorma.contactnotes.ui.navigation.ArchivedRoute
import com.alorma.contactnotes.ui.navigation.CustomChannelsRoute
import com.alorma.contactnotes.ui.navigation.EditPersonRoute
import com.alorma.contactnotes.ui.navigation.EnterWordsRoute
import com.alorma.contactnotes.ui.navigation.OnboardingRoute
import com.alorma.contactnotes.ui.navigation.PaywallRoute
import com.alorma.contactnotes.ui.navigation.PeopleRoute
import com.alorma.contactnotes.ui.navigation.PersonLockedRoute
import com.alorma.contactnotes.ui.navigation.PersonRoute
import com.alorma.contactnotes.ui.navigation.QuickNoteRoute
import com.alorma.contactnotes.ui.navigation.SecurityWordsRoute
import com.alorma.contactnotes.ui.navigation.SettingsRoute
import com.alorma.contactnotes.ui.navigation.SyncRoute
import com.alorma.contactnotes.ui.navigation.TaskRoute
import com.alorma.contactnotes.ui.paywall.ProBenefit

/** Les pantalles de l'app. Mai porten ids ni text: només valors tancats. */
object Screens {
  class Onboarding : Screen("onboarding", 1)

  class People : Screen("people", 1)

  class Archived : Screen("archived", 1)

  /** També la d'una persona protegida, un cop desbloquejada: compta igual que qualsevol altra. */
  class Person : Screen("person", 1)

  class EditPerson(
    creating: Boolean,
  ) : Screen("edit_person", 1, mapOf("mode" to mode(creating)))

  class QuickNote(
    withPerson: Boolean,
    shared: Boolean,
  ) : Screen(
      "quick_note",
      1,
      mapOf("with_person" to withPerson.toString(), "source" to if (shared) "share" else "app"),
    )

  class Task(
    creating: Boolean,
  ) : Screen("task", 1, mapOf("mode" to mode(creating)))

  class Paywall(
    highlight: ProBenefit?,
  ) : Screen("paywall", 1, mapOf("highlight" to (highlight?.name?.lowercase() ?: "none")))

  class Settings : Screen("settings", 1)

  class CustomChannels : Screen("custom_channels", 1)

  /** Sense l'estat del compte: si l'usuari sincronitza o no, no es diu. */
  class Sync : Screen("sync", 1)

  class SyncWords(
    activation: Boolean,
  ) : Screen("sync_words", 1, mapOf("mode" to if (activation) "activation" else "review"))

  class SyncEnterWords : Screen("sync_enter_words", 1)
}

private fun mode(creating: Boolean) = if (creating) "create" else "edit"

/**
 * La [Screen] de cada ruta. `null` per a [PersonLockedRoute]: registrar-la diria que l'usuari té persones protegides.
 * El text compartit de [QuickNoteRoute] no s'envia mai; només si n'hi ha.
 */
fun AppRoute.toScreen(): Screen? =
  when (this) {
    OnboardingRoute -> Screens.Onboarding()
    PeopleRoute -> Screens.People()
    ArchivedRoute -> Screens.Archived()
    is PersonRoute -> Screens.Person()
    is PersonLockedRoute -> null
    is EditPersonRoute -> Screens.EditPerson(creating = personId == 0L)
    is QuickNoteRoute -> Screens.QuickNote(withPerson = personId != 0L, shared = sharedText != null)
    is TaskRoute -> Screens.Task(creating = taskId == 0L)
    is PaywallRoute -> Screens.Paywall(highlight)
    SettingsRoute -> Screens.Settings()
    CustomChannelsRoute -> Screens.CustomChannels()
    SyncRoute -> Screens.Sync()
    is SecurityWordsRoute -> Screens.SyncWords(activation)
    EnterWordsRoute -> Screens.SyncEnterWords()
  }

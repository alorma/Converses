package com.alorma.contactnotes.ui.tracking

import com.alorma.contactnotes.data.sync.ActivationOutcome
import com.alorma.contactnotes.data.tracking.Action
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.ui.entry.EntryChannel

/**
 * Les accions de l'usuari. Els paràmetres són enums i booleans perquè no s'hi pugui colar text, noms ni ids: un canal
 * propi només diu que ho és (`custom_channel`), mai quin. Obrir una pantalla ja ho registra [Screens]; aquí només hi
 * ha el que canvia alguna cosa o obre un full o un diàleg.
 */
object Actions {
  // Llista de persones

  class FilterToggled(
    channel: ChannelType,
    selected: Boolean,
  ) : Action("filter_toggled", 1, mapOf("channel" to channel.tracked(), "selected" to selected.toString()))

  // Detall d'una persona

  class KindFilterToggled(
    kind: EntryKind,
  ) : Action("kind_filter_toggled", 1, mapOf("kind" to kind.tracked()))

  class EntrySheetOpened(
    mode: EntryMode,
  ) : Action("entry_sheet_opened", 1, mapOf("mode" to mode.tracked()))

  class EntrySaved(
    mode: EntryMode,
    kind: EntryKind,
    channel: EntryChannel,
  ) : Action(
      "entry_saved",
      1,
      mapOf(
        "mode" to mode.tracked(),
        "kind" to kind.tracked(),
        "channel" to channel.group.tracked(),
        "custom_channel" to (channel.customChannelId != null).toString(),
      ),
    )

  class EntryMenuOpened : Action("entry_menu_opened", 1)

  class EntryCopied : Action("entry_copied", 1)

  class EntryMoved : Action("entry_moved", 1)

  class EntryDeleted : Action("entry_deleted", 1)

  class EntryRestored : Action("entry_restored", 1)

  class DayDeleted : Action("day_deleted", 1)

  class PersonExported : Action("person_exported", 1)

  class PersonArchived(
    archived: Boolean,
  ) : Action("person_archived", 1, mapOf("archived" to archived.toString()))

  class TaskDone : Action("task_done", 1)

  class TaskReopened(
    fromUndo: Boolean,
  ) : Action("task_reopened", 1, mapOf("source" to if (fromUndo) "undo" else "list"))

  // Nota ràpida

  class PersonPickerOpened : Action("person_picker_opened", 1)

  // Editor de persona

  class PersonSaved(
    creating: Boolean,
  ) : Action("person_saved", 1, mapOf("mode" to if (creating) "create" else "edit"))

  /** Només quan canvia en desar, no a cada toc de l'interruptor. Mai diu de quina persona. */
  class PersonProtectionChanged(
    enabled: Boolean,
  ) : Action("person_protection_changed", 1, mapOf("enabled" to enabled.toString()))

  class PersonDeleted : Action("person_deleted", 1)

  class AvatarSheetOpened : Action("avatar_sheet_opened", 1)

  /** Sense forma ni color. */
  class AvatarApplied : Action("avatar_applied", 1)

  // Tasca

  class TaskSaved(
    creating: Boolean,
    hasDue: Boolean,
  ) : Action("task_saved", 1, mapOf("mode" to if (creating) "create" else "edit", "has_due" to hasDue.toString()))

  class TaskDeleted : Action("task_deleted", 1)

  // Canals propis

  class CustomChannelSaved(
    creating: Boolean,
    group: ChannelType,
  ) : Action(
      "custom_channel_saved",
      1,
      mapOf("mode" to if (creating) "create" else "edit", "channel" to group.tracked()),
    )

  class CustomChannelDeleted : Action("custom_channel_deleted", 1)

  // Ajustos

  class SettingChanged private constructor(
    setting: String,
    value: String,
  ) : Action("setting_changed", 1, mapOf("setting" to setting, "value" to value)) {
    companion object {
      fun theme(mode: ThemeMode) = SettingChanged("theme", mode.name.lowercase())

      fun toggle(
        setting: ToggleSetting,
        enabled: Boolean,
      ) = SettingChanged(setting.name.lowercase(), enabled.toString())

      /** Els segons són una de les opcions fixes del selector. */
      fun relockAfter(seconds: Int) = SettingChanged("relock_after", seconds.toString())
    }
  }

  class LinkOpened(
    target: LinkTarget,
  ) : Action("link_opened", 1, mapOf("target" to target.name.lowercase()))

  // Paywall

  class PurchaseStarted : Action("purchase_started", 1)

  class RestoreTapped : Action("restore_tapped", 1)

  // Onboarding

  class OnboardingFinished(
    skipped: Boolean,
  ) : Action("onboarding_finished", 1, mapOf("skipped" to skipped.toString()))

  // Sincronització. Mai el compte, el correu ni el nombre de paraules: només què ha passat.

  class SyncSignInStarted : Action("sync_sign_in_started", 1)

  /** `result`: `activating` (primer dispositiu), `needs_key`, `cancelled`, `offline` o `failed`. */
  class SyncSignInFinished(
    result: SyncSignInResult,
  ) : Action("sync_sign_in_finished", 1, mapOf("result" to result.tracked()))

  class SyncActivationFinished(
    result: ActivationOutcome,
  ) : Action("sync_activation_finished", 1, mapOf("result" to result.tracked()))

  /** Tancar les paraules o cancel·lar l'empremta abans de «Continua»: la sessió es tanca. */
  class SyncActivationCancelled : Action("sync_activation_cancelled", 1)

  class SyncWordsCopied : Action("sync_words_copied", 1)

  /** `result`: `accepted`, `wrong`, `invalid`, `offline` o `failed`. */
  class SyncWordsSubmitted(
    result: SyncWordsResult,
  ) : Action("sync_words_submitted", 1, mapOf("result" to result.tracked()))

  /** `from`: `ready` («Tanca la sessió») o `needs_key` («Canvia», sense les paraules). */
  class SyncSignedOut(
    needsKey: Boolean,
  ) : Action("sync_signed_out", 1, mapOf("from" to if (needsKey) "needs_key" else "ready"))
}

enum class SyncSignInResult { ACTIVATING, NEEDS_KEY, CANCELLED, OFFLINE, FAILED }

enum class SyncWordsResult { ACCEPTED, WRONG, INVALID, OFFLINE, FAILED }

/** Com s'escriu una entrada: nova, nova amb el tipus ja triat (entrada fantasma), editada o des de la nota ràpida. */
enum class EntryMode { NEW, NEW_AS, EDIT, QUICK }

enum class ToggleSetting { DYNAMIC_COLOR, PERSON_COLOR, HAPTICS, APP_LOCK, ANALYTICS }

enum class LinkTarget { LANGUAGE, SECURITY, PRIVACY, FEEDBACK }

/** `AlreadyActive` surt com `already_active`, igual que la resta de valors. */
private fun Enum<*>.tracked(): String = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), "_").lowercase()

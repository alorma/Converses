package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.ui.preview.PreviewData

private val Channels = PreviewData.martaChannels

@Composable
private fun QuickNotePreview(
  state: QuickNoteUiState,
  form: EntryFormState,
  customChannels: CustomChannelsUi = CustomChannelsUi(),
) {
  ConversesPreviewTheme {
    QuickNoteContent(
      state = state,
      form = form,
      customChannels = customChannels,
      onClose = {},
      onSelectPerson = {},
      onNewPerson = {},
      onSave = { _, _ -> },
    )
  }
}

@Preview
@Composable
internal fun QuickNoteNoPersonPreview() {
  QuickNotePreview(
    state = QuickNoteUiState(people = listOf(PreviewData.marta)),
    form = EntryFormState(whenChoice = WhenChoice.Stored(PreviewData.formTime)),
  )
}

@Preview
@Composable
internal fun QuickNoteFilledPreview() {
  QuickNotePreview(
    state =
      QuickNoteUiState(
        people = listOf(PreviewData.marta),
        selected = PreviewData.marta,
        lastChannel = EntryChannel(Channels.first()),
      ),
    form =
      EntryFormState(
        kind = EntryKind.HEARD,
        text = "She can't come on Saturday.",
        whenChoice = WhenChoice.Custom(PreviewData.formTime),
      ),
  )
}

private val PreviewCustomChannels =
  listOf("WhatsApp", "Telegram", "Discord").mapIndexed { index, name ->
    CustomChannelEntity(id = index + 1L, channelType = ChannelType.CHAT, name = name, createdAt = 0)
  }

/** Canals propis amb Pro (7a): el grup Xat, amb WhatsApp triat. */
@Preview
@Composable
internal fun QuickNoteCustomChannelsPreview() {
  QuickNotePreview(
    state = QuickNoteUiState(people = listOf(PreviewData.marta), selected = PreviewData.marta),
    form =
      EntryFormState(
        kind = EntryKind.SAID,
        channel = EntryChannel(ChannelType.CHAT, customChannelId = 1),
        text = "Told her I can't make it to dinner on Saturday.",
        whenChoice = WhenChoice.Stored(PreviewData.formTime),
      ),
    customChannels = CustomChannelsUi(isPro = true, channels = PreviewCustomChannels),
  )
}

/** Canals propis sense Pro (7c): «Canal propi» amb PRO. */
@Preview
@Composable
internal fun QuickNoteCustomChannelsLockedPreview() {
  QuickNotePreview(
    state = QuickNoteUiState(people = listOf(PreviewData.marta), selected = PreviewData.marta),
    form =
      EntryFormState(
        kind = EntryKind.SAID,
        channel = EntryChannel(ChannelType.CHAT),
        text = "Told her I can't make it to dinner on Saturday.",
        whenChoice = WhenChoice.Stored(PreviewData.formTime),
      ),
    customChannels = CustomChannelsUi(isPro = false, channels = PreviewCustomChannels),
  )
}

/** Text importat d'una altra app, encara sense persona triada («Qui» a «Jo»). */
@Preview
@Composable
internal fun QuickNoteSharedPreview() {
  QuickNotePreview(
    state = QuickNoteUiState(people = listOf(PreviewData.marta)),
    form =
      EntryFormState(
        text = "She can't come on Saturday.",
        whenChoice = WhenChoice.Stored(PreviewData.formTime),
      ),
  )
}

/** Text importat en una instal·lació nova: la targeta convida a crear la primera persona. */
@Preview
@Composable
internal fun QuickNoteSharedNoPeoplePreview() {
  QuickNotePreview(
    state = QuickNoteUiState(),
    form =
      EntryFormState(
        text = "She can't come on Saturday.",
        whenChoice = WhenChoice.Stored(PreviewData.formTime),
      ),
  )
}

/** «Qui» a «Jo» (el valor per defecte) amb la data d'una entrada desada (mode edició). */
@Preview
@Composable
internal fun EditEntrySheetPreview() {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.color) {
      Surface(color = MaterialTheme.colorScheme.surface) {
        NewEntrySheetContent(
          person = PreviewData.marta,
          form =
            EntryFormState(
              kind = EntryKind.SAID,
              text = "Told her I can't make it to dinner on Saturday.",
              whenChoice = WhenChoice.Stored(PreviewData.formTime),
            ),
          channel = EntryChannel(Channels.first()),
          isNew = false,
          onDismiss = {},
          onSave = {},
        )
      }
    }
  }
}

@Preview
@Composable
internal fun NewEntrySheetPreview() {
  ConversesPreviewTheme {
    PersonTheme(PreviewData.marta.color) {
      Surface(color = MaterialTheme.colorScheme.surface) {
        NewEntrySheetContent(
          person = PreviewData.marta,
          form =
            EntryFormState(
              kind = EntryKind.HAPPENED,
              text = "We went to the photography exhibition.",
              whenChoice = WhenChoice.Stored(PreviewData.formTime),
            ),
          channel = EntryChannel(Channels.first()),
          isNew = true,
          onDismiss = {},
          onSave = {},
        )
      }
    }
  }
}

@Composable
private fun WhenSheetPreview(choice: WhenChoice) {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        WhenSheetContent(choice = choice, now = PreviewData.formTime, onChoice = {}, onPick = {})
      }
    }
  }
}

@Preview
@Composable
internal fun WhenSheetNowPreview() = WhenSheetPreview(WhenChoice.Now)

@Preview
@Composable
internal fun WhenSheetCustomPreview() =
  WhenSheetPreview(
    WhenChoice.Custom(
      PreviewData.formTime - 3 * 24 * 60 * 60 * 1000L,
    ),
  )

@Composable
private fun CustomChannelSheetPreview(
  isNew: Boolean,
  name: String,
  takenIn: ChannelType? = null,
) {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      CustomChannelSheetContent(
        isNew = isNew,
        name = name,
        onNameChange = {},
        group = ChannelType.CHAT,
        onGroupChange = {},
        takenIn = takenIn,
        onCancel = {},
        onSave = {},
        modifier = Modifier.padding(top = 16.dp),
      )
    }
  }
}

/** Full «Nou canal» (7b). */
@Preview
@Composable
internal fun CustomChannelSheetNewPreview() {
  CustomChannelSheetPreview(isNew = true, name = "Signal")
}

/** Mateix full per reanomenar i canviar de grup, amb el nom repetit al grup. */
@Preview
@Composable
internal fun CustomChannelSheetTakenPreview() {
  CustomChannelSheetPreview(isNew = false, name = "WhatsApp", takenIn = ChannelType.CHAT)
}

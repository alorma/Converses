package com.alorma.contactnotes.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme

private object NoOpEditPersonActions : EditPersonActions {
  override fun onName(value: String) = Unit
  override fun onAlias(value: String) = Unit
  override fun onShape(shape: AvatarShape) = Unit
  override fun onColor(color: AvatarColor) = Unit
  override fun onProtected(value: Boolean) = Unit
  override fun onAddFact() = Unit
  override fun onFactChange(key: Long, label: String, value: String) = Unit
  override fun onRemoveFact(key: Long) = Unit
  override fun onMoveFact(from: Int, to: Int) = Unit
  override fun onSaveChannel(key: Long?, type: ChannelType, handle: String) = Unit
  override fun onRemoveChannel(key: Long) = Unit
  override fun delete(exportFirst: Boolean, onDeleted: () -> Unit) = Unit
  override fun save(onSaved: (Long) -> Unit) = Unit
}

@Composable
private fun EditPersonPreview(state: EditPersonUiState, canAuthenticate: Boolean = true) {
  ConversesPreviewTheme {
    EditPersonContent(
      state = state,
      canAuthenticate = canAuthenticate,
      actions = NoOpEditPersonActions,
      onClose = {},
      onSaved = {},
      onDeleted = {},
      onProtectedChange = {},
    )
  }
}

private val Facts = listOf(
  FactDraft(key = 1, label = "Birthday", value = "14 March"),
  FactDraft(key = 2, label = "Work", value = "Designer in Barcelona"),
)

private val Channels = listOf(
  ChannelDraft(key = 3, type = ChannelType.WHATSAPP, handle = "+34 600 111 222"),
  ChannelDraft(key = 4, type = ChannelType.IN_PERSON, handle = ""),
)

@Preview
@Composable
internal fun EditPersonNewPreview() {
  EditPersonPreview(EditPersonUiState(isNew = true, loaded = true))
}

@Preview
@Composable
internal fun EditPersonExistingPreview() {
  val snapshot = EditSnapshot(
    name = "Marta Soler",
    alias = "Marta",
    shape = AvatarShape.Cookie9Sided,
    color = AvatarColor.Lavender,
    facts = Facts,
    channels = Channels,
  )
  EditPersonPreview(
    EditPersonUiState(
      isNew = false,
      loaded = true,
      name = snapshot.name,
      alias = snapshot.alias,
      shape = snapshot.shape,
      color = snapshot.color,
      facts = Facts,
      channels = Channels,
      entryCount = 3,
      baseline = snapshot,
    ),
  )
}

/** Persona protegida amb un àlies que ja existeix, i el color de l'avatar com a tema. */
@Preview
@Composable
internal fun EditPersonProtectedPreview() {
  EditPersonPreview(
    EditPersonUiState(
      isNew = false,
      loaded = true,
      name = "Luna Roja",
      alias = "Marta",
      shape = AvatarShape.Sunny,
      color = AvatarColor.Rose,
      isProtected = true,
      aliasTaken = true,
      facts = Facts.take(1),
      channels = Channels.take(1),
      entryCount = 1,
    ),
  )
}

@Composable
private fun EditorDialogPreview(content: @Composable () -> Unit) {
  ConversesPreviewTheme {
    PersonTheme(AvatarColor.Lavender) { content() }
  }
}

@Preview
@Composable
internal fun DeletePersonDialogPreview() {
  EditorDialogPreview {
    DeletePersonDialog(name = "Marta Soler", factsCount = 2, entriesCount = 3, onDismiss = {}, onConfirm = {})
  }
}

@Preview
@Composable
internal fun ChannelDialogPreview() {
  EditorDialogPreview {
    ChannelDialog(initial = Channels.first(), onDismiss = {}, onConfirm = { _, _ -> })
  }
}

package com.alorma.contactnotes.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape

private object NoOpEditPersonActions : EditPersonActions {
  override fun onName(value: String) = Unit

  override fun onAlias(value: String) = Unit

  override fun onShape(shape: AvatarShape) = Unit

  override fun onColor(color: AvatarColor?) = Unit

  override fun onTrialShape(shape: AvatarShape) = Unit

  override fun onTrialColor(color: AvatarColor) = Unit

  override fun onDiscardTrial() = Unit

  override fun onUnlockTrial() = Unit

  override fun onAvatarSheetReopened() = Unit

  override fun onApplyTrial() = Unit

  override fun onProtected(value: Boolean) = Unit

  override fun onAddFact() = Unit

  override fun onFactChange(
    key: Long,
    label: String,
    value: String,
  ) = Unit

  override fun onRemoveFact(key: Long) = Unit

  override fun onMoveFact(
    from: Int,
    to: Int,
  ) = Unit

  override fun delete(
    exportFirst: Boolean,
    onDeleted: () -> Unit,
  ) = Unit

  override fun save(onSaved: (Long) -> Unit) = Unit
}

@Composable
private fun EditPersonPreview(
  state: EditPersonUiState,
  canAuthenticate: Boolean = true,
  isPro: Boolean = true,
) {
  ConversesPreviewTheme {
    EditPersonContent(
      state = state,
      canAuthenticate = canAuthenticate,
      actions = NoOpEditPersonActions,
      onClose = {},
      onSaved = {},
      onDeleted = {},
      onProtectedChange = {},
      isPro = isPro,
    )
  }
}

private val Facts =
  listOf(
    FactDraft(key = 1, label = "Birthday", value = "14 March"),
    FactDraft(key = 2, label = "Work", value = "Designer in Barcelona"),
  )

@Preview
@Composable
internal fun EditPersonNewPreview() {
  EditPersonPreview(EditPersonUiState(isNew = true, loaded = true))
}

@Preview
@Composable
internal fun EditPersonExistingPreview() {
  val snapshot =
    EditSnapshot(
      name = "Marta Soler",
      alias = "Marta",
      shape = AvatarShape.Cookie9Sided,
      color = AvatarColor.Lavender,
      facts = Facts,
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

/** Amb Pro: l'avatar és un botó amb la insígnia d'edició. */
@Preview
@Composable
internal fun EditPersonCustomAvatarPreview() {
  EditPersonPreview(
    EditPersonUiState(
      isNew = false,
      loaded = true,
      name = "Marta Soler",
      shape = AvatarShape.Clover4Leaf,
      color = AvatarColor.Sky,
      facts = Facts.take(1),
      entryCount = 3,
    ),
    isPro = true,
  )
}

@Composable
private fun AvatarSheetPreview(
  shape: AvatarShape?,
  color: AvatarColor?,
  lock: AvatarSheetLock? = null,
) {
  ConversesPreviewTheme {
    androidx.compose.material3.Surface(
      color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      AvatarSheetContent(
        name = "Marta Soler",
        shape = shape,
        color = color,
        onShape = {},
        onColor = {},
        onDone = {},
        lock = lock,
      )
    }
  }
}

/** Sense Pro (11b): PRO, targeta «Formes i colors amb Pro» i cadenats; Galeta i Automàtic són lliures. */
@Preview
@Composable
internal fun AvatarSheetLockedPreview() = AvatarSheetPreview(shape = null, color = null, lock = AvatarSheetLock())

/** Sense Pro, provant una forma i un color Pro (11c): «Així quedaria», «Desbloqueja amb Pro» i «Descarta la prova». */
@Preview
@Composable
internal fun AvatarSheetTrialPreview() =
  AvatarSheetPreview(
    shape = null,
    color = null,
    lock = AvatarSheetLock(trialShape = AvatarShape.Clover4Leaf, trialColor = AvatarColor.Sky),
  )

/** Ha deixat de ser Pro (11d): la forma i el color desats es poden mantenir, sense cadenat. */
@Preview
@Composable
internal fun AvatarSheetDowngradePreview() =
  AvatarSheetPreview(
    shape = AvatarShape.Clover8Leaf,
    color = AvatarColor.Lavender,
    lock = AvatarSheetLock(savedShape = AvatarShape.Clover8Leaf, savedColor = AvatarColor.Lavender),
  )

/** Forma i color per defecte: el resum diu «color automàtic». */
@Preview
@Composable
internal fun AvatarSheetAutoPreview() = AvatarSheetPreview(shape = null, color = null)

@Preview
@Composable
internal fun AvatarSheetCustomPreview() = AvatarSheetPreview(shape = AvatarShape.Clover8Leaf, color = AvatarColor.Rose)

/** Sense Pro (11a): la píndola PRO en lloc del llapis; l'avatar es pot tocar i obre el full. */
@Preview
@Composable
internal fun EditPersonLockedAvatarPreview() {
  EditPersonPreview(
    EditPersonUiState(
      isNew = false,
      loaded = true,
      name = "Helena",
      alias = "Dax",
      entryCount = 3,
    ),
    isPro = false,
  )
}

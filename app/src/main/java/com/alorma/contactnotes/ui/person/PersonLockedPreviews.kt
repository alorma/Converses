package com.alorma.contactnotes.ui.person

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.designsystem.theme.PersonTheme

@Composable
private fun PersonLockedPreview(state: LockState) {
  ConversesPreviewTheme {
    PersonTheme(AvatarColor.Lavender) {
      PersonLockedContent(
        name = "Luna Roja",
        shape = AvatarShape.Cookie9Sided,
        color = AvatarColor.Lavender,
        state = state,
        onBack = {},
        onRetry = {},
        onSetup = {},
      )
    }
  }
}

@Preview
@Composable
internal fun PersonLockedWaitingPreview() = PersonLockedPreview(LockState.Waiting)

@Preview
@Composable
internal fun PersonLockedCanceledPreview() = PersonLockedPreview(LockState.Canceled())

@Preview
@Composable
internal fun PersonLockedLockedOutPreview() = PersonLockedPreview(LockState.LockedOut(seconds = 24))

@Preview
@Composable
internal fun PersonLockedNoCredentialsPreview() = PersonLockedPreview(LockState.NoCredentials)

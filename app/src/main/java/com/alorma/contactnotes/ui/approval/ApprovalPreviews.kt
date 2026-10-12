package com.alorma.contactnotes.ui.approval

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Composable
private fun ApprovalPreview(state: ApprovalUiState) {
  ConversesPreviewTheme {
    ApprovalContent(state = state, onConfirm = {}, onDeny = {}, onClose = {})
  }
}

@Preview
@Composable
internal fun ApprovalConfirmPreview() = ApprovalPreview(ApprovalUiState.Confirm)

@Preview
@Composable
internal fun ApprovalDonePreview() = ApprovalPreview(ApprovalUiState.Done)

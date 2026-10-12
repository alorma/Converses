package com.alorma.contactnotes.ui.approval

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Preview
@Composable
internal fun ApprovalConfirmPreview() {
  ConversesPreviewTheme {
    ApprovalContent(onConfirm = {}, onDeny = {}, onClose = {})
  }
}

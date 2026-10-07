package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Indicador de càrrega expressiu amb contenidor. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConversesLoadingIndicator(modifier: Modifier = Modifier, size: Dp = 56.dp) {
  ContainedLoadingIndicator(modifier = modifier.size(size))
}

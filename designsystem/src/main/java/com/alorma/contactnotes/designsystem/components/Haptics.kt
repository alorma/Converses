package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.RowScope

/**
 * Única font del feedback haptic de l'app. Confirma accions amb efecte (`confirm`), marca errors (`reject`)
 * i acompanya interruptors i canvis de segment. Mai al scroll, a la navegació ni al plegat de la barra.
 * El sistema ja el silencia si l'usuari té el haptic desactivat.
 */
@Stable
class ConversesHaptics(private val feedback: HapticFeedback) {
  fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

  fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

  fun toggle(on: Boolean) =
    feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

  fun segmentTick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

  fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberConversesHaptics(): ConversesHaptics {
  val feedback = LocalHapticFeedback.current
  return remember(feedback) { ConversesHaptics(feedback) }
}

/**
 * Botó de desar: `Confirm` en tocar-lo i `Reject` si està desactivat i es toca.
 * Un botó desactivat no rep clics, per això el toc es detecta al contenidor sense consumir-lo.
 */
@Composable
fun ConversesSaveButton(
  onClick: () -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  colors: ButtonColors = ButtonDefaults.buttonColors(),
  content: @Composable RowScope.() -> Unit,
) {
  val haptics = rememberConversesHaptics()
  Box(
    modifier = Modifier.pointerInput(enabled) {
      if (!enabled) detectTapGestures { haptics.reject() }
    },
  ) {
    Button(
      modifier = modifier,
      onClick = {
        haptics.confirm()
        onClick()
      },
      enabled = enabled,
      colors = colors,
      content = content,
    )
  }
}

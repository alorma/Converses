package com.alorma.contactnotes.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Composable
private fun OnboardingPreview(page: Int) {
  ConversesPreviewTheme {
    OnboardingContent(onFinish = {}, initialPage = page, forceReducedMotion = true)
  }
}

/** Estat final de cada pas (moviment reduït). */
@Preview
@Composable
internal fun OnboardingStep1Preview() = OnboardingPreview(0)

@Preview
@Composable
internal fun OnboardingStep2Preview() = OnboardingPreview(1)

@Preview
@Composable
internal fun OnboardingStep3Preview() = OnboardingPreview(2)

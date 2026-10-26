package com.alorma.contactnotes.ui.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme

@Composable
private fun PaywallPreview(
  state: PaywallState,
  highlight: ProBenefit? = null,
) {
  ConversesPreviewTheme {
    PaywallContent(
      state = state,
      highlight = highlight,
      onClose = {},
      onBuy = {},
      onRestore = {},
      onPrivacy = {},
    )
  }
}

/** 8a: des de la targeta d'Ajustos. */
@Preview
@Composable
internal fun PaywallPreview() {
  PaywallPreview(PaywallState(price = "4,99 €"))
}

/** 8b: des de Contactes, el benefici d'origen va primer i ressaltat. */
@Preview
@Composable
internal fun PaywallFromContactsPreview() {
  PaywallPreview(PaywallState(price = "4,99 €"), highlight = ProBenefit.Contacts)
}

/** Sense connexió: el botó es desactiva i ho diu. */
@Preview
@Composable
internal fun PaywallOfflinePreview() {
  PaywallPreview(PaywallState(isOnline = false))
}

/** 8c: «Ja tens Pro». */
@Preview
@Composable
internal fun PaywallOwnedPreview() {
  PaywallPreview(PaywallState(isPro = true), highlight = ProBenefit.Contacts)
}

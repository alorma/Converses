package com.alorma.contactnotes.data.pro

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseResultTest {
  private fun error(code: PurchasesErrorCode) = PurchasesError(code, null)

  @Test
  fun userCancelledIsACancellationWhateverTheCode() {
    assertEquals(
      PurchaseResult.Cancelled,
      error(PurchasesErrorCode.UnknownError).toPurchaseResult(userCancelled = true),
    )
  }

  @Test
  fun cancelledCodeIsACancellation() {
    assertEquals(
      PurchaseResult.Cancelled,
      error(PurchasesErrorCode.PurchaseCancelledError).toPurchaseResult(userCancelled = false),
    )
  }

  @Test
  fun pendingPayment() {
    assertEquals(
      PurchaseResult.Pending,
      error(PurchasesErrorCode.PaymentPendingError).toPurchaseResult(userCancelled = false),
    )
  }

  @Test
  fun networkErrorIsOffline() {
    assertEquals(PurchaseResult.Offline, error(PurchasesErrorCode.NetworkError).toPurchaseResult(userCancelled = false))
    assertEquals(RestoreResult.Offline, error(PurchasesErrorCode.NetworkError).toRestoreResult())
  }

  @Test
  fun anythingElseFails() {
    assertEquals(
      PurchaseResult.Failed,
      error(PurchasesErrorCode.StoreProblemError).toPurchaseResult(userCancelled = false),
    )
    assertEquals(RestoreResult.Failed, error(PurchasesErrorCode.StoreProblemError).toRestoreResult())
  }
}

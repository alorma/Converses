package com.alorma.contactnotes.data.pro

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

/** Com acaba una compra de Pro. La cancel·lació no porta cap missatge (disseny, «Estats de la compra»). */
sealed interface PurchaseResult {
  data object Purchased : PurchaseResult

  /** Pagament pendent (per exemple, en efectiu): Pro arribarà pel listener quan es cobri. */
  data object Pending : PurchaseResult

  data object Cancelled : PurchaseResult

  data object Offline : PurchaseResult

  data object Failed : PurchaseResult
}

sealed interface RestoreResult {
  data object Restored : RestoreResult

  data object NothingToRestore : RestoreResult

  data object Offline : RestoreResult

  data object Failed : RestoreResult
}

internal fun PurchasesError.toPurchaseResult(userCancelled: Boolean): PurchaseResult =
  when {
    userCancelled || code == PurchasesErrorCode.PurchaseCancelledError -> PurchaseResult.Cancelled
    code == PurchasesErrorCode.PaymentPendingError -> PurchaseResult.Pending
    code == PurchasesErrorCode.NetworkError -> PurchaseResult.Offline
    else -> PurchaseResult.Failed
  }

internal fun PurchasesError.toRestoreResult(): RestoreResult =
  if (code == PurchasesErrorCode.NetworkError) RestoreResult.Offline else RestoreResult.Failed

package com.alorma.contactnotes.data.pro

import android.app.Activity
import android.content.Context
import android.os.Looper
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.getCustomerInfoWith

/**
 * Sense `appUserID`: RevenueCat fa servir el seu identificador anònim i no rep mai notes ni persones. Es configura quan
 * el fil principal queda lliure, després del primer frame: al `onCreate` afegia uns 400 ms a l'arrencada.
 */
class RevenueCatGateway(
  private val context: Context,
  private val apiKey: String,
  private val debugLogs: Boolean,
) : PurchasesGateway {
  override fun start(onEntitlement: (Boolean) -> Unit) {
    Looper.getMainLooper().queue.addIdleHandler {
      if (debugLogs) Purchases.logLevel = LogLevel.DEBUG
      val purchases = Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
      purchases.updatedCustomerInfoListener = { info -> onEntitlement(info.isPro()) }
      // Si falla (sense connexió), no es diu res: es manté l'últim valor conegut
      purchases.getCustomerInfoWith(onSuccess = { info -> onEntitlement(info.isPro()) })
      false
    }
  }

  override suspend fun price(): String? =
    try {
      proPackage()?.product?.price?.formatted
    } catch (_: PurchasesException) {
      null
    }

  override suspend fun purchase(activity: Activity): PurchaseResult {
    if (!Purchases.isConfigured) return PurchaseResult.Failed
    val pack =
      try {
        proPackage()
      } catch (e: PurchasesException) {
        return e.error.toPurchaseResult(userCancelled = false)
      } ?: return PurchaseResult.Failed
    return try {
      val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, pack).build())
      if (result.customerInfo.isPro()) PurchaseResult.Purchased else PurchaseResult.Pending
    } catch (e: PurchasesTransactionException) {
      e.error.toPurchaseResult(e.userCancelled)
    }
  }

  override suspend fun restore(): RestoreResult {
    if (!Purchases.isConfigured) return RestoreResult.Failed
    return try {
      if (Purchases.sharedInstance.awaitRestore().isPro()) RestoreResult.Restored else RestoreResult.NothingToRestore
    } catch (e: PurchasesTransactionException) {
      e.error.toRestoreResult()
    }
  }

  /** L'únic paquet de l'offering actual; `null` si l'SDK encara no està configurat o no hi ha offering. */
  private suspend fun proPackage() =
    if (Purchases.isConfigured) {
      Purchases.sharedInstance
        .awaitOfferings()
        .current
        ?.availablePackages
        ?.firstOrNull()
    } else {
      null
    }
}

private fun CustomerInfo.isPro(): Boolean = entitlements[PRO_ENTITLEMENT]?.isActive == true

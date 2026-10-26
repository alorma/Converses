package com.alorma.contactnotes.data.pro

import android.content.Context
import android.os.Looper
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
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
}

private fun CustomerInfo.isPro(): Boolean = entitlements[PRO_ENTITLEMENT]?.isActive == true

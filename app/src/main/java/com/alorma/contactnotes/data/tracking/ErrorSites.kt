package com.alorma.contactnotes.data.tracking

/**
 * Els noms tancats del `where` de [Tracker.trackError]: diuen on ha passat l'error, mai amb quines dades. Només hi ha
 * errors que l'app s'empassa; el que fa petar l'app no passa per aquí (Crashlytics, fase 8).
 */
object ErrorSites {
  // Pro (RevenueCat). Sense xarxa, cancel·lat o pendent no són errors: ja surten com a esdeveniments.

  /** No s'ha pogut llegir el preu del paquet Pro. */
  const val PRO_PRICE = "pro_price"

  /** No s'ha pogut llegir el `CustomerInfo` en arrencar; es manté l'últim valor conegut. */
  const val PRO_CUSTOMER_INFO = "pro_customer_info"

  const val PRO_PURCHASE = "pro_purchase"

  const val PRO_RESTORE = "pro_restore"

  // Obrir fora de l'app

  /** Sense navegador per obrir un enllaç (Ajustos o la política de privacitat del paywall). */
  const val OPEN_LINK = "open_link"

  /** Sense app de correu per enviar comentaris. */
  const val OPEN_FEEDBACK = "open_feedback"

  /** El sistema no té l'alta de biometria i s'obre la configuració de seguretat. */
  const val OPEN_SECURITY_SETUP = "open_security_setup"

  // DataStore corrupte: s'ha buidat i l'app ha arrencat amb els valors per defecte. Si és `settings`, el
  // consentiment també s'ha esborrat i l'error no surt del mòbil.

  const val DATASTORE_SETTINGS = "datastore_settings"

  const val DATASTORE_PRO = "datastore_pro"

  const val DATASTORE_FLAGS = "datastore_flags"

  /** El dispositiu queda sense sessió de sincronització; les dades i la clau no es toquen. */
  const val DATASTORE_SYNC = "datastore_sync"

  // Sincronització. Sense xarxa o tancar el full de Google no són errors.

  /** L'inici (o el tancament) de sessió amb Google ha fallat per un motiu que no és la xarxa ni l'usuari. */
  const val SYNC_SIGN_IN = "sync_sign_in"

  /** No s'ha pogut llegir o crear el document del compte, o el que hi ha no té una clau vàlida. */
  const val SYNC_ACCOUNT = "sync_account"

  /** El fitxer de la clau del dispositiu no es pot obrir o desar; caldran les paraules de seguretat. */
  const val SYNC_KEY_STORE = "sync_key_store"
}

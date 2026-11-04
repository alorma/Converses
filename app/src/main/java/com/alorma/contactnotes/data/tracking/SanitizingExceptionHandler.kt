package com.alorma.contactnotes.data.tracking

/**
 * Es posa al damunt del handler de Crashlytics perquè els crashes fatals també surtin sense `message`, que pot
 * portar dades de l'usuari: [delegate] (Crashlytics, que després crida el handler del sistema) rep la còpia de
 * [FirebaseTracker.sanitize]. [logOriginal] deixa l'excepció original al logcat, que no surt del mòbil.
 */
class SanitizingExceptionHandler(
  private val delegate: Thread.UncaughtExceptionHandler?,
  private val logOriginal: (Throwable) -> Unit = {},
) : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(
    thread: Thread,
    error: Throwable,
  ) {
    runCatching { logOriginal(error) }
    delegate?.uncaughtException(thread, FirebaseTracker.sanitize(error))
  }
}

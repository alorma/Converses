package com.alorma.contactnotes.ui.sync

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Copia les paraules de seguretat marcades com a sensibles (el sistema no les mostra a la vista prèvia) i les esborra
 * al cap d'un minut. El `Handler` és del fil principal i no de la pantalla: l'esborrat s'ha de fer encara que
 * l'usuari ja hagi sortit.
 */
object SensitiveClipboard {
  const val ClearAfterMillis = 60_000L
  private const val Label = "security-words"

  private val handler = Handler(Looper.getMainLooper())
  private var pendingClear: Runnable? = null

  fun copy(
    context: Context,
    text: String,
  ) {
    val clipboard = context.applicationContext.getSystemService(ClipboardManager::class.java) ?: return
    val clip = ClipData.newPlainText(Label, text)
    clip.description.extras =
      PersistableBundle().apply {
        putBoolean(
          if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE",
          true,
        )
      }
    clipboard.setPrimaryClip(clip)
    pendingClear?.let(handler::removeCallbacks)
    val clear = Runnable { clearIfStillThere(clipboard, text) }
    pendingClear = clear
    handler.postDelayed(clear, ClearAfterMillis)
  }

  /** Si l'usuari ha copiat una altra cosa en el minut, no s'esborra. */
  private fun clearIfStillThere(
    clipboard: ClipboardManager,
    text: String,
  ) {
    pendingClear = null
    val current =
      runCatching {
        clipboard.primaryClip
          ?.takeIf { it.itemCount > 0 }
          ?.getItemAt(0)
          ?.text
          ?.toString()
      }.getOrNull()
    // Amb l'app en segon pla Android no deixa llegir el porta-retalls (null): llavors s'esborra igualment
    if (current != null && current != text) return
    if (Build.VERSION.SDK_INT >= 28) {
      clipboard.clearPrimaryClip()
    } else {
      clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }
  }
}

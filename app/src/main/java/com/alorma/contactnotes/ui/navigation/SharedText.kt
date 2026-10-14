package com.alorma.contactnotes.ui.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey

/**
 * Text coming from another app: `ACTION_SEND` (`EXTRA_TEXT`, with an optional `EXTRA_SUBJECT` in front)
 * or `ACTION_PROCESS_TEXT` (`EXTRA_PROCESS_TEXT`). Returns `null` when there is nothing to import.
 */
fun parseSharedText(
  action: String?,
  text: CharSequence?,
  subject: CharSequence?,
  processText: CharSequence?,
): String? {
  val body =
    when (action) {
      Intent.ACTION_SEND -> text?.toString()?.trim().orEmpty()
      Intent.ACTION_PROCESS_TEXT -> processText?.toString()?.trim().orEmpty()
      else -> return null
    }
  val title = if (action == Intent.ACTION_SEND) subject?.toString()?.trim().orEmpty() else ""
  val joined =
    when {
      title.isEmpty() || title in body -> body
      body.isEmpty() -> title
      else -> "$title\n\n$body"
    }
  return joined.takeIf { it.isNotEmpty() }
}

/** `true` when [text] is a single http(s) link and nothing else. */
fun isOnlyUrl(text: String): Boolean {
  val value = text.trim()
  return value.isNotEmpty() &&
    value.none { it.isWhitespace() } &&
    (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) &&
    value.length > value.indexOf("://") + 3
}

/** Initial back stack for shared [text]: the quick note alone, or `null` when the launch is not an import. */
fun sharedTextBackStack(text: String?): List<NavKey>? = text?.let { listOf(QuickNoteRoute(sharedText = it)) }

/** Converts a share / select-text intent into the initial back stack; `null` for any other launch. */
fun Intent.toBackStack(): List<NavKey>? =
  sharedTextBackStack(
    parseSharedText(
      action = action,
      text = getCharSequenceExtra(Intent.EXTRA_TEXT),
      subject = getCharSequenceExtra(Intent.EXTRA_SUBJECT),
      processText = getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT),
    ),
  )

/** `true` for the intents [toBackStack] understands, even when their text turns out to be empty. */
val Intent.isTextImport: Boolean get() = action == Intent.ACTION_SEND || action == Intent.ACTION_PROCESS_TEXT

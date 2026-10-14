package com.alorma.contactnotes.ui.entry

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alorma.contactnotes.R

private fun ClipboardManager.hasText(): Boolean =
  hasPrimaryClip() && primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

/**
 * Whether the clipboard holds plain text. It only looks at the clip's description: the content is not read
 * (so Android 12+ shows no «pasted from clipboard» toast) until [readClipboardText] is called on a tap.
 */
@Composable
fun rememberClipboardHasText(): State<Boolean> {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val state = remember { mutableStateOf(false) }
  DisposableEffect(lifecycleOwner) {
    // Sense servei de porta-retalls (previews i tests de screenshots) simplement no hi ha botó
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (clipboard == null) return@DisposableEffect onDispose {}
    val refresh = { state.value = clipboard.hasText() }
    val listener = ClipboardManager.OnPrimaryClipChangedListener { refresh() }
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
    refresh()
    clipboard.addPrimaryClipChangedListener(listener)
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      clipboard.removePrimaryClipChangedListener(listener)
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
  return state
}

/** Reads the clipboard text (the only place the content is accessed); `null` when there is none. */
fun readClipboardText(context: Context): String? {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  return clipboard.primaryClip
    ?.takeIf { it.itemCount > 0 }
    ?.getItemAt(0)
    ?.coerceToText(context)
    ?.toString()
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
}

/** Appends [pasted] to [current] (after a blank line when there already is text). */
fun pasteInto(
  current: String,
  pasted: String,
): String = if (current.isBlank()) pasted else current.trimEnd() + "\n\n" + pasted

/**
 * Round outlined icon-only paste button at the bottom end of the text field (its [EntryTextField] footer); only
 * shown when the clipboard has text. The icon carries the description TalkBack reads, and `OutlinedIconButton`
 * keeps the button role and the 48dp minimum touch target.
 */
@Composable
fun PasteButton(
  onPasted: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hasText by rememberClipboardHasText()
  if (!hasText) return
  val context = LocalContext.current
  Row(modifier = modifier.fillMaxWidth().padding(end = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {
    OutlinedIconButton(onClick = { readClipboardText(context)?.let(onPasted) }) {
      Icon(
        Icons.Outlined.ContentPaste,
        contentDescription = stringResource(R.string.paste),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

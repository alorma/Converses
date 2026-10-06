package com.alorma.contactnotes.ui.person

import android.net.Uri
import com.alorma.contactnotes.data.db.ChannelType

/** Enllaç directe a la conversa, si el canal en té i hi ha un usuari desat. */
fun channelLink(type: ChannelType, handle: String?): Uri? {
  val value = handle?.trim().orEmpty()
  if (value.isEmpty()) return null
  return when (type) {
    ChannelType.WHATSAPP -> value.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.let { Uri.parse("https://wa.me/$it") }
    ChannelType.TELEGRAM -> Uri.parse("https://t.me/${value.removePrefix("@")}")
    ChannelType.PHONE -> Uri.fromParts("tel", value, null)
    else -> null
  }
}

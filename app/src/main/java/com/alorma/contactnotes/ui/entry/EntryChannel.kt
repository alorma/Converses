package com.alorma.contactnotes.ui.entry

import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.shared.ChannelType

/** Camp «On» d'una entrada: el grup i, opcionalment, un canal propi d'aquell grup (Pro). */
data class EntryChannel(
  val group: ChannelType,
  val customChannelId: Long? = null,
) {
  /** Canviar de grup desmarca el canal propi; tornar a tocar el mateix grup no el perd. */
  fun withGroup(group: ChannelType): EntryChannel = if (group == this.group) this else EntryChannel(group)

  /** Tocar el canal triat el desmarca; tocar-ne un altre el tria (i el seu grup). */
  fun toggle(channel: CustomChannelEntity): EntryChannel =
    if (customChannelId == channel.id) copy(customChannelId = null) else EntryChannel(channel.channelType, channel.id)

  companion object {
    fun of(entry: EntryEntity): EntryChannel = EntryChannel(entry.channelType, entry.customChannelId)
  }
}

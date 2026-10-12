package com.alorma.contactnotes.wear.ui

import android.content.Context
import android.text.format.DateFormat
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.wear.R
import java.util.Date
import java.util.Locale

/** Els textos del mapatge, des dels recursos del rellotge (mateixes claus que el mòbil quan n'hi ha). */
class AndroidWearLabels(
  private val context: Context,
) : WearLabels {
  override val me: String get() = context.getString(R.string.entry_who_me)
  override val note: String get() = context.getString(R.string.entry_who_note)

  override fun channel(type: ChannelType): String =
    context.getString(
      when (type) {
        ChannelType.IN_PERSON -> R.string.channel_in_person
        ChannelType.PHONE -> R.string.channel_phone
        ChannelType.CHAT -> R.string.channel_chat
        ChannelType.SOCIAL -> R.string.channel_social
        ChannelType.OTHER -> R.string.channel_other
      },
    )

  override fun relative(time: RelativeTime): String =
    when (time) {
      RelativeTime.JustNow -> context.getString(R.string.just_now)
      is RelativeTime.Minutes -> context.getString(R.string.minutes_ago, time.count)
      is RelativeTime.Hours -> context.getString(R.string.hours_ago, time.count)
      RelativeTime.Yesterday -> context.getString(R.string.yesterday)
      is RelativeTime.Date ->
        DateFormat
          .format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "d MMM"), Date(time.timestamp))
          .toString()
    }

  override fun clock(timestamp: Long): String = DateFormat.getTimeFormat(context).format(Date(timestamp))
}

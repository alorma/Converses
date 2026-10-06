package com.alorma.contactnotes.ui.person

import android.content.Context
import android.content.Intent
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.ui.people.label
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.first

/** Genera una còpia en text pla de la persona: dades fixades, canals i cronologia. */
class PersonExporter(
  private val context: Context,
  private val repository: ConversesRepository,
) {
  suspend fun buildText(personId: Long): String? {
    val person = repository.observePerson(personId).first() ?: return null
    val channels = repository.observeChannels(personId).first()
    val facts = repository.observePinnedFacts(personId).first()
    val entries = repository.getEntries(personId)
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    val zone = ZoneId.systemDefault()

    return buildString {
      appendLine(person.name + person.alias?.let { " (@$it)" }.orEmpty())
      if (channels.isNotEmpty()) {
        appendLine(
          channels.joinToString(" · ") { channel ->
            context.getString(channel.type.label()) + channel.handle?.let { ": $it" }.orEmpty()
          },
        )
      }
      if (facts.isNotEmpty()) {
        appendLine()
        facts.forEach { appendLine("${it.label}: ${it.value}") }
      }
      if (entries.isNotEmpty()) {
        appendLine()
        entries.asReversed().forEach { entry -> appendLine(entry.line(formatter, zone)) }
      }
    }.trimEnd()
  }

  private fun EntryEntity.line(formatter: DateTimeFormatter, zone: ZoneId): String {
    val whenText = Instant.ofEpochMilli(occurredAt).atZone(zone).format(formatter)
    return "[$whenText · ${context.getString(kind.label())} · ${context.getString(channelType.label())}] $text"
  }

  fun share(name: String, text: String) {
    val send = Intent(Intent.ACTION_SEND)
      .setType("text/plain")
      .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_title, name))
      .putExtra(Intent.EXTRA_TEXT, text)
    val chooser = Intent.createChooser(send, context.getString(R.string.export_title, name))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }
}

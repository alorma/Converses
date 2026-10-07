package com.alorma.contactnotes.ui.person

import android.content.Context
import android.content.Intent
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.EntryEntity
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
          channels.map { it.type }.sorted().joinToString(" · ") { channelName(context, it) },
        )
      }
      if (facts.isNotEmpty()) {
        appendLine()
        facts.forEach { appendLine("${it.label}: ${it.value}") }
      }
      if (entries.isNotEmpty()) {
        appendLine()
        val name = firstName(person.name)
        entries.asReversed().forEach { entry -> appendLine(entry.line(name, formatter, zone)) }
      }
    }.trimEnd()
  }

  private fun EntryEntity.line(firstName: String, formatter: DateTimeFormatter, zone: ZoneId): String {
    val whenText = Instant.ofEpochMilli(occurredAt).atZone(zone).format(formatter)
    return "[$whenText · ${channelName(context, channelType)}] ${whoLabel(context, kind, firstName)}: $text"
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

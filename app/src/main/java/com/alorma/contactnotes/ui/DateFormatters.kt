package com.alorma.contactnotes.ui

import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val formatters = ConcurrentHashMap<Locale, ConcurrentHashMap<String, DateTimeFormatter>>()

/**
 * `DateTimeFormatter` per a un patró i una llengua, creat una sola vegada. `ofPattern` torna a analitzar el patró a cada
 * crida, i els formatadors es fan servir a cada fila de les llistes.
 */
internal fun dateFormatter(
  pattern: String,
  locale: Locale,
): DateTimeFormatter {
  val byPattern = formatters.getOrPut(locale) { ConcurrentHashMap() }
  return byPattern[pattern] ?: DateTimeFormatter.ofPattern(pattern, locale).also { byPattern[pattern] = it }
}

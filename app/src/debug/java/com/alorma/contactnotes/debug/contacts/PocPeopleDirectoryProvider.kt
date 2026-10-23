package com.alorma.contactnotes.debug.contacts

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.edit

// PROVA DE CONCEPTE (fase 0 de docs/contacts): no es fusiona mai a main.
// Constants de android.provider.PeopleContract (API 37.2), copiades per no haver de pujar el compileSdk.
private const val QUERY_ARG_QUERIES = "android:query-arg-queries"
private const val QUERY_ARG_SELECTION_TYPE = "android:query-arg-selection-type"
private const val QUERY_ARG_SELECTION_VALUE = "android:query-arg-selection-value"
private const val TEXT_REACHABILITY = 4

/** Persones vinculades: telèfons i correus del contacte → id i nom. Només per a la prova. */
object PocDirectoryStore {
  private fun prefs(context: Context) = context.getSharedPreferences("contacts_poc", Context.MODE_PRIVATE)

  fun save(
    context: Context,
    personId: Long,
    name: String,
    values: List<String>,
  ) = prefs(context).edit {
    values.forEach { putString(normalize(it), "$personId|$name") }
  }

  fun find(
    context: Context,
    value: String,
  ): Pair<Long, String>? =
    prefs(context).getString(normalize(value), null)?.split('|', limit = 2)?.let { it[0].toLong() to it[1] }

  private fun normalize(value: String): String {
    val isEmail = '@' in value
    return if (isEmail) value.lowercase() else value.filter { it.isDigit() }.takeLast(9)
  }
}

class PocPeopleDirectoryProvider : ContentProvider() {
  override fun onCreate(): Boolean = true

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    queryArgs: Bundle?,
    cancellationSignal: CancellationSignal?,
  ): Cursor {
    Log.i(POC_TAG, "directory query uri=$uri projection=${projection?.toList()} args=${queryArgs?.dump()}")
    val columns =
      projection
        ?: arrayOf(
          "lookup_key",
          "display_name",
          "photo_uri",
          "capability",
          QUERY_ARG_SELECTION_TYPE,
          QUERY_ARG_SELECTION_VALUE,
        )
    val cursor = MatrixCursor(columns)
    if (uri.pathSegments.firstOrNull() != "persons") return cursor

    @Suppress("DEPRECATION")
    val queries = queryArgs?.getParcelableArrayList<Bundle>(QUERY_ARG_QUERIES).orEmpty()
    queries.forEach { query ->
      val type = query.getString(QUERY_ARG_SELECTION_TYPE)
      val values =
        query.getStringArray(QUERY_ARG_SELECTION_VALUE)
          ?: query.getString(QUERY_ARG_SELECTION_VALUE)?.let { arrayOf(it) }
      values.orEmpty().forEach { value ->
        val match = context?.let { PocDirectoryStore.find(it, value) } ?: return@forEach
        val row =
          mapOf(
            "lookup_key" to "person-${match.first}",
            "display_name" to match.second,
            "photo_uri" to null,
            "capability" to TEXT_REACHABILITY,
            QUERY_ARG_SELECTION_TYPE to type,
            QUERY_ARG_SELECTION_VALUE to value,
          )
        cursor.addRow(columns.map { row[it] })
      }
    }
    Log.i(POC_TAG, "directory returns ${cursor.count} rows")
    return cursor
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor? = query(uri, projection, Bundle(), null)

  override fun getType(uri: Uri): String? = null

  override fun insert(
    uri: Uri,
    values: ContentValues?,
  ): Uri? = null

  override fun delete(
    uri: Uri,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = 0

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = 0
}

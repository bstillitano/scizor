package com.scizor.feature.databasebrowser

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/** Columns + rows read from a table (values stringified for display). */
internal data class TableData(
    val columns: List<String>,
    val rows: List<List<String>>,
)

/**
 * Read-only browser over the app's SQLite databases (including Room). Lists the
 * databases, their tables, and a page of rows per table.
 */
internal object DatabaseBrowser {

    fun databases(context: Context): List<String> =
        context.databaseList()
            .filterNot { it.endsWith("-journal") || it.endsWith("-wal") || it.endsWith("-shm") }
            .sorted()

    fun tables(context: Context, dbName: String): List<String> =
        openReadOnly(context, dbName)?.use { db ->
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
                null,
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        } ?: emptyList()

    fun rows(context: Context, dbName: String, table: String, limit: Int = 100): TableData =
        openReadOnly(context, dbName)?.use { db ->
            db.rawQuery("SELECT * FROM \"$table\" LIMIT $limit", null).use { cursor ->
                val columns = cursor.columnNames.toList()
                val rows = buildList {
                    while (cursor.moveToNext()) {
                        add((0 until cursor.columnCount).map { valueAt(cursor, it) })
                    }
                }
                TableData(columns, rows)
            }
        } ?: TableData(emptyList(), emptyList())

    private fun openReadOnly(context: Context, dbName: String): SQLiteDatabase? = runCatching {
        SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }.getOrNull()

    private fun valueAt(cursor: Cursor, index: Int): String = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> "null"
        Cursor.FIELD_TYPE_BLOB -> "<blob>"
        else -> cursor.getString(index) ?: "null"
    }
}

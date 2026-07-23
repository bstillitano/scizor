package com.scizor.feature.databasebrowser

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

internal data class DbInfo(val name: String, val sizeBytes: Long)
internal data class TableInfo(val name: String, val isView: Boolean)
internal data class ColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val primaryKey: Boolean,
    val default: String?,
)
internal data class Schema(
    val columns: List<ColumnInfo>,
    val foreignKeys: List<String>,
    val indexes: List<String>,
)

/** Columns + rows read from a table (values stringified for display). */
internal data class TableData(val columns: List<String>, val rows: List<List<String>>)

/** A typed cell value for writes, so NULL, integers, reals and blobs round-trip correctly. */
internal sealed interface CellValue {
    object Null : CellValue
    data class Text(val value: String) : CellValue
    data class Integer(val value: Long) : CellValue
    data class Real(val value: Double) : CellValue
    /** Raw bytes, entered/edited as base64. */
    data class Blob(val bytes: ByteArray) : CellValue
}

/** Result of a raw SQL execution. */
internal data class QueryResult(
    val data: TableData? = null,
    val rowsAffected: Int = 0,
    val error: String? = null,
    val readOnly: Boolean = true,
    val executionMs: Long = 0,
)

/** Browser over the app's SQLite databases (including Room), with read/write support. */
internal object DatabaseBrowser {

    fun databases(context: Context): List<DbInfo> =
        context.databaseList()
            .filterNot { it.endsWith("-journal") || it.endsWith("-wal") || it.endsWith("-shm") }
            .sorted()
            .map { DbInfo(it, context.getDatabasePath(it).length()) }

    fun tables(context: Context, dbName: String): List<TableInfo> =
        readOnly(context, dbName)?.use { db ->
            db.rawQuery(
                "SELECT name, type FROM sqlite_master WHERE type IN ('table','view') " +
                    "AND name NOT LIKE 'sqlite_%' ORDER BY name",
                null,
            ).use { c ->
                buildList { while (c.moveToNext()) add(TableInfo(c.getString(0), c.getString(1) == "view")) }
            }
        } ?: emptyList()

    fun count(context: Context, dbName: String, table: String): Int =
        readOnly(context, dbName)?.use { db ->
            runCatching {
                db.rawQuery("SELECT COUNT(*) FROM \"$table\"", null).use { c ->
                    if (c.moveToFirst()) c.getInt(0) else 0
                }
            }.getOrDefault(0)
        } ?: 0

    fun rows(context: Context, dbName: String, table: String, limit: Int = 50, offset: Int = 0): TableData =
        readOnly(context, dbName)?.use { db ->
            db.rawQuery("SELECT * FROM \"$table\" LIMIT $limit OFFSET $offset", null).use { c ->
                TableData(c.columnNames.toList(), readRows(c))
            }
        } ?: TableData(emptyList(), emptyList())

    fun schema(context: Context, dbName: String, table: String): Schema =
        readOnly(context, dbName)?.use { db ->
            val columns = db.rawQuery("PRAGMA table_info(\"$table\")", null).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            ColumnInfo(
                                name = c.getString(c.getColumnIndexOrThrow("name")),
                                type = c.getString(c.getColumnIndexOrThrow("type")),
                                notNull = c.getInt(c.getColumnIndexOrThrow("notnull")) == 1,
                                primaryKey = c.getInt(c.getColumnIndexOrThrow("pk")) > 0,
                                default = c.getString(c.getColumnIndexOrThrow("dflt_value")),
                            ),
                        )
                    }
                }
            }
            val fks = db.rawQuery("PRAGMA foreign_key_list(\"$table\")", null).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val from = c.getString(c.getColumnIndexOrThrow("from"))
                        val toTable = c.getString(c.getColumnIndexOrThrow("table"))
                        val to = c.getString(c.getColumnIndexOrThrow("to"))
                        add("$from → $toTable.$to")
                    }
                }
            }
            val indexes = db.rawQuery("PRAGMA index_list(\"$table\")", null).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val name = c.getString(c.getColumnIndexOrThrow("name"))
                        val unique = c.getInt(c.getColumnIndexOrThrow("unique")) == 1
                        add(if (unique) "$name (unique)" else name)
                    }
                }
            }
            Schema(columns, fks, indexes)
        } ?: Schema(emptyList(), emptyList(), emptyList())

    fun execute(context: Context, dbName: String, sql: String): QueryResult {
        val trimmed = sql.trim()
        val readOnly = trimmed.substringBefore(' ').uppercase() in setOf("SELECT", "PRAGMA", "EXPLAIN")
        val db = readWrite(context, dbName) ?: return QueryResult(error = "Cannot open database")
        val start = android.os.SystemClock.elapsedRealtime()
        return db.use {
            runCatching {
                if (readOnly) {
                    it.rawQuery(trimmed, null).use { c ->
                        val data = TableData(c.columnNames.toList(), readRows(c))
                        QueryResult(data, readOnly = true, executionMs = elapsed(start))
                    }
                } else {
                    it.execSQL(trimmed)
                    QueryResult(rowsAffected = changes(it), readOnly = false, executionMs = elapsed(start))
                }
            }.getOrElse { e -> QueryResult(error = e.message ?: "SQL error", executionMs = elapsed(start)) }
        }
    }

    /** Rows changed by the most recent statement on this connection. */
    private fun changes(db: SQLiteDatabase): Int = runCatching {
        db.rawQuery("SELECT changes()", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }.getOrDefault(0)

    private fun elapsed(start: Long): Long = android.os.SystemClock.elapsedRealtime() - start

    fun insertRow(context: Context, dbName: String, table: String, values: Map<String, CellValue>): Boolean =
        readWrite(context, dbName)?.use {
            runCatching { it.insert("\"$table\"", null, contentValues(values)) >= 0 }.getOrDefault(false)
        } ?: false

    fun deleteRow(context: Context, dbName: String, table: String, pkColumn: String, pkValue: String): Boolean =
        readWrite(context, dbName)?.use {
            runCatching { it.delete("\"$table\"", "\"$pkColumn\" = ?", arrayOf(pkValue)) > 0 }.getOrDefault(false)
        } ?: false

    fun updateRow(
        context: Context,
        dbName: String,
        table: String,
        pkColumn: String,
        pkValue: String,
        values: Map<String, CellValue>,
    ): Boolean = readWrite(context, dbName)?.use {
        runCatching {
            it.update("\"$table\"", contentValues(values), "\"$pkColumn\" = ?", arrayOf(pkValue)) > 0
        }.getOrDefault(false)
    } ?: false

    private fun contentValues(values: Map<String, CellValue>): ContentValues = ContentValues().apply {
        values.forEach { (k, v) ->
            when (v) {
                is CellValue.Null -> putNull(k)
                is CellValue.Text -> put(k, v.value)
                is CellValue.Integer -> put(k, v.value)
                is CellValue.Real -> put(k, v.value)
                is CellValue.Blob -> put(k, v.bytes)
            }
        }
    }

    private fun readRows(cursor: Cursor): List<List<String>> = buildList {
        while (cursor.moveToNext()) {
            add((0 until cursor.columnCount).map { valueAt(cursor, it) })
        }
    }

    private fun readOnly(context: Context, dbName: String): SQLiteDatabase? = runCatching {
        SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READONLY)
    }.getOrNull()

    private fun readWrite(context: Context, dbName: String): SQLiteDatabase? = runCatching {
        SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READWRITE)
    }.getOrNull()

    private fun valueAt(cursor: Cursor, index: Int): String = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> "null"
        Cursor.FIELD_TYPE_BLOB -> "<blob ${cursor.getBlob(index)?.size ?: 0} bytes>"
        else -> cursor.getString(index) ?: "null"
    }
}

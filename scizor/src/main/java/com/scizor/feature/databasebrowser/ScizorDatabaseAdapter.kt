package com.scizor.feature.databasebrowser

/**
 * A host-provided, read-only database source shown in the Database Browser's
 * "Custom databases" section — the Android counterpart to Scyther's database
 * adapter protocol. Implement this to expose non-SQLite stores (Realm, an
 * in-memory cache, a remote snapshot, etc.) alongside the app's SQLite files.
 *
 * Register instances via `Scizor.databaseAdapters`.
 */
interface ScizorDatabaseAdapter {
    /** Display name for the database. */
    val name: String

    /** Names of the browsable tables/collections. */
    val tables: List<String>

    /** Column/field names for [table], in display order. */
    fun columns(table: String): List<String>

    /** Total row count for [table]. */
    fun count(table: String): Int

    /** A page of rows for [table]; each row's cells align with [columns]. */
    fun rows(table: String, limit: Int, offset: Int): List<List<String>>
}

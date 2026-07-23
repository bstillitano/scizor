package com.scizor.feature.databasebrowser

/** No-op mirror of the real [ScizorDatabaseAdapter]. */
interface ScizorDatabaseAdapter {
    val name: String
    val tables: List<String>
    fun columns(table: String): List<String>
    fun count(table: String): Int
    fun rows(table: String, limit: Int, offset: Int): List<List<String>>
}

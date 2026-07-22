package com.scizor.feature.databasebrowser

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabaseBrowserTest {

    @Test
    fun `lists databases, tables and rows`() {
        val context = RuntimeEnvironment.getApplication()
        context.openOrCreateDatabase("test.db", Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS t (id INTEGER PRIMARY KEY, name TEXT)")
            db.execSQL("DELETE FROM t")
            db.execSQL("INSERT INTO t (name) VALUES ('alpha')")
        }

        assertTrue(DatabaseBrowser.databases(context).contains("test.db"))
        assertTrue(DatabaseBrowser.tables(context, "test.db").contains("t"))

        val data = DatabaseBrowser.rows(context, "test.db", "t")
        assertTrue(data.columns.contains("name"))
        assertTrue(data.rows.any { it.contains("alpha") })
    }
}

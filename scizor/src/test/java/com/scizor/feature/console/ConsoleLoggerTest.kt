package com.scizor.feature.console

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConsoleLoggerTest {

    @After
    fun tearDown() = ConsoleLogger.clear()

    @Test
    fun `parseLine extracts fields from threadtime format`() {
        val line = "07-22 11:30:01.123  1234  1250 D ScizorTag: hello world"
        val entry = ConsoleLogger.parseLine(line)!!

        assertEquals("07-22 11:30:01.123", entry.time)
        assertEquals(LogLevel.DEBUG, entry.level)
        assertEquals("ScizorTag", entry.tag)
        assertEquals("hello world", entry.message)
    }

    @Test
    fun `parseLine maps error level`() {
        val line = "07-22 11:30:01.123  1234  1250 E Boom: crashed"
        assertEquals(LogLevel.ERROR, ConsoleLogger.parseLine(line)!!.level)
    }

    @Test
    fun `parseLine returns null for non-matching lines`() {
        assertNull(ConsoleLogger.parseLine("--------- beginning of main"))
    }

    @Test
    fun `buffer caps at max entries dropping oldest`() {
        ConsoleLogger.clear()
        repeat(5001) { i ->
            ConsoleLogger.append(LogEntry("t", LogLevel.INFO, "Tag", "msg $i"))
        }
        val entries = ConsoleLogger.entries.value
        assertEquals(5000, entries.size)
        // Oldest ("msg 0") evicted; the window now starts at "msg 1".
        assertEquals("msg 1", entries.first().message)
        assertEquals("msg 5000", entries.last().message)
    }
}

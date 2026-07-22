package com.scizor.feature.filebrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileBrowserTest {

    @Test
    fun `roots include the files directory`() {
        val context = RuntimeEnvironment.getApplication()
        assertTrue(FileBrowser.roots(context).any { it.label == "Files" })
    }

    @Test
    fun `lists and reads a written file`() {
        val context = RuntimeEnvironment.getApplication()
        File(context.filesDir, "hello.txt").writeText("hi there")

        val node = FileBrowser.list(context.filesDir).first { it.name == "hello.txt" }
        assertFalse(node.isDirectory)
        assertEquals("hi there", FileBrowser.readText(node.file))
    }
}

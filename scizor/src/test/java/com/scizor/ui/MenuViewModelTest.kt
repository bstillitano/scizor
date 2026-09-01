package com.scizor.ui

import com.scizor.Scizor
import com.scizor.feature.custom.DeveloperOption
import com.scizor.feature.custom.ScizorIcon
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MenuViewModelTest {

    @Before
    fun setUp() {
        Scizor.start(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        Scizor.developerOptions = emptyList()
    }

    @Test
    fun `device and application facts are rendered inline`() {
        val context = RuntimeEnvironment.getApplication()
        val groups = MenuViewModel().groups(context, null, emptyList())

        val device = groups.first { it.title == "Device" }
        assertTrue(device.rows.filterIsInstance<MenuRow.Info>().any { it.label == "Model" })

        val application = groups.first { it.title == "Application" }
        assertTrue(application.rows.filterIsInstance<MenuRow.Info>().any { it.label == "Package" })
    }

    @Test
    fun `feature screens are grouped into sections`() {
        val context = RuntimeEnvironment.getApplication()
        val titles = MenuViewModel().groups(context, null, emptyList())
            .flatMap { it.rows }
            .filterIsInstance<MenuRow.Action>()
            .map { it.title }

        assertTrue(titles.contains("Network Logger"))
        assertTrue(titles.contains("Environment Variables"))
        assertTrue(titles.contains("Feature Flags"))
    }

    @Test
    fun `developer options appear in a Development Tools section right after Application`() {
        val context = RuntimeEnvironment.getApplication()
        Scizor.developerOptions = listOf(
            DeveloperOption.Action(title = "Reset onboarding") {},
        )

        val groups = MenuViewModel().groups(context, null, emptyList())
        val devIndex = groups.indexOfFirst { it.title == "Development Tools" }
        val appIndex = groups.indexOfFirst { it.title == "Application" }
        assertEquals(appIndex + 1, devIndex)
        assertTrue(
            groups[devIndex].rows.filterIsInstance<MenuRow.Action>().any { it.title == "Reset onboarding" },
        )
    }

    @Test
    fun `a value option renders as an info row`() {
        val context = RuntimeEnvironment.getApplication()
        Scizor.developerOptions = listOf(
            DeveloperOption.Value(title = "Build", value = "1234"),
        )

        val rows = MenuViewModel().groups(context, null, emptyList())
            .first { it.title == "Development Tools" }.rows

        val info = rows.filterIsInstance<MenuRow.Info>().single()
        assertEquals("Build", info.label)
        assertEquals("1234", info.value)
    }

    @Test
    fun `a screen option renders as an action that opens`() {
        val context = RuntimeEnvironment.getApplication()
        Scizor.developerOptions = listOf(
            DeveloperOption.Screen(title = "Sandbox", screen = {}),
        )

        val rows = MenuViewModel().groups(context, null, emptyList())
            .first { it.title == "Development Tools" }.rows

        val action = rows.filterIsInstance<MenuRow.Action>().single()
        assertEquals("Sandbox", action.title)
        assertTrue(action.action is MenuAction.Open)
    }

    @Test
    fun `a toggle option renders as a toggle row that reads the host on every read`() {
        val context = RuntimeEnvironment.getApplication()
        var hostState = false
        Scizor.developerOptions = listOf(
            DeveloperOption.Toggle(
                title = "Bypass PIN rules",
                checked = { hostState },
                onCheckedChange = { hostState = it },
            ),
        )

        val toggle = MenuViewModel().groups(context, null, emptyList())
            .first { it.title == "Development Tools" }.rows
            .filterIsInstance<MenuRow.Toggle>().single()

        assertEquals("Bypass PIN rules", toggle.title)
        assertFalse(toggle.checked!!())

        // The host changes its own state; the row must reflect it without rebuilding.
        hostState = true
        assertTrue(toggle.checked!!())

        // And writing through the row reaches the host.
        toggle.onChange(false)
        assertFalse(hostState)
    }

    @Test
    fun `subtitles reach the rendered row`() {
        val context = RuntimeEnvironment.getApplication()
        Scizor.developerOptions = listOf(
            DeveloperOption.Action(title = "Crash", subtitle = "Throws immediately") {},
        )

        val action = MenuViewModel().groups(context, null, emptyList())
            .first { it.title == "Development Tools" }.rows
            .filterIsInstance<MenuRow.Action>().single()

        assertEquals("Throws immediately", action.subtitle)
    }
}

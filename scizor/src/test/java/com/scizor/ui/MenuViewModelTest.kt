package com.scizor.ui

import com.scizor.Scizor
import com.scizor.feature.custom.DeveloperOption
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
            DeveloperOption(title = "Reset onboarding") {},
        )

        val groups = MenuViewModel().groups(context, null, emptyList())
        val devIndex = groups.indexOfFirst { it.title == "Development Tools" }
        val appIndex = groups.indexOfFirst { it.title == "Application" }
        assertTrue(devIndex == appIndex + 1)
        assertTrue(
            groups[devIndex].rows.filterIsInstance<MenuRow.Action>().any { it.title == "Reset onboarding" },
        )

        Scizor.developerOptions = emptyList()
    }
}

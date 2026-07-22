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
    fun `built-in features are grouped into sections`() {
        val groups = MenuViewModel().groups()
        val titles = groups.flatMap { it.items }.map { it.title }
        assertTrue(titles.contains("Device & App Info"))
        assertTrue(titles.contains("Network Logger"))
        assertTrue(titles.contains("Environment Variables"))
    }

    @Test
    fun `developer options appear in a Developer section`() {
        Scizor.developerOptions = listOf(
            DeveloperOption(title = "Reset onboarding") {},
        )

        val groups = MenuViewModel().groups()
        val developer = groups.first { it.title == "Developer" }
        assertTrue(developer.items.any { it.title == "Reset onboarding" })

        Scizor.developerOptions = emptyList()
    }
}

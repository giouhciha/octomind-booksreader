package com.octomind.booksreader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OctomindLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun applicationOpensAndReachesLibrary() {
        val confirmAdult = composeRule.activity.getString(R.string.confirm_adult)
        val libraryBrand = composeRule.activity.getString(R.string.library_brand)
        val libraryTitle = composeRule.activity.getString(R.string.library_title)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(confirmAdult).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(libraryBrand).fetchSemanticsNodes().isNotEmpty()
        }

        if (composeRule.onAllNodesWithText(confirmAdult).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(confirmAdult).performClick()
        }

        composeRule.onNodeWithText(libraryBrand).assertIsDisplayed()
        composeRule.onNodeWithText(libraryTitle).assertIsDisplayed()
    }
}

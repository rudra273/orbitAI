package com.example.orbitai.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.orbitai.BuildConfig
import androidx.compose.ui.test.performClick
import com.example.orbitai.feature.update.WhatsNew
import com.example.orbitai.feature.update.WhatsNewDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WhatsNewDialogTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun showsTheHeadlineFeaturesAndDismisses() {
        var dismissed = false
        rule.setContent { WhatsNewDialog { dismissed = true } }

        rule.onNodeWithText("What's new in Orbit ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        rule.onNodeWithText(WhatsNew.items.first().first).assertIsDisplayed()
        rule.onNodeWithText("Got it").performClick()
        assertTrue(dismissed)
    }
}

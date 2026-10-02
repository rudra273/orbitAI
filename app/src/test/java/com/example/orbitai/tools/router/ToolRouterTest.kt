package com.example.orbitai.tools.router

import com.example.orbitai.feature.automation.AutomationRouter
import com.example.orbitai.feature.automation.AutomationRoute
import com.example.orbitai.feature.automation.parser.AutomationRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationRouterTest {

    @Test
    fun routesDraftEmailCommandToToolFlow() {
        val result = AutomationRouter.route("draft email job application follow up")

        assertEquals(
            AutomationRoute.ToolOnly(AutomationRequest.DraftEmail(topicHint = "job application follow up")),
            result,
        )
    }

    @Test
    fun routesRegularTextToNormalChat() {
        val result = AutomationRouter.route("explain android intents")

        assertEquals(AutomationRoute.NormalChat, result)
    }

    @Test
    fun routesWhatsAppCommandToToolFlow() {
        val result = AutomationRouter.route("whatsapp tell him I am on my way")

        assertEquals(
            AutomationRoute.ToolOnly(AutomationRequest.DraftWhatsApp(topicHint = "tell him I am on my way")),
            result,
        )
    }

    @Test
    fun routesReminderCommandToToolFlow() {
        val result = AutomationRouter.route("remind me to pay rent tomorrow at 9 am")

        assertEquals(
            AutomationRoute.ToolOnly(AutomationRequest.CreateReminder(topicHint = "pay rent tomorrow at 9 am")),
            result,
        )
    }
}

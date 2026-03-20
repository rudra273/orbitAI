package com.example.orbitai.tools.router

import com.example.orbitai.tools.intents.IntentToolRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolRouterTest {

    @Test
    fun routesDraftEmailCommandToToolFlow() {
        val result = ToolRouter.route("draft email job application follow up")

        assertEquals(
            ToolRoute.ToolOnly(IntentToolRequest.DraftEmail(topicHint = "job application follow up")),
            result,
        )
    }

    @Test
    fun routesRegularTextToNormalChat() {
        val result = ToolRouter.route("explain android intents")

        assertEquals(ToolRoute.NormalChat, result)
    }

    @Test
    fun routesWhatsAppCommandToToolFlow() {
        val result = ToolRouter.route("whatsapp tell him I am on my way")

        assertEquals(
            ToolRoute.ToolOnly(IntentToolRequest.DraftWhatsApp(topicHint = "tell him I am on my way")),
            result,
        )
    }

    @Test
    fun routesReminderCommandToToolFlow() {
        val result = ToolRouter.route("remind me to pay rent tomorrow at 9 am")

        assertEquals(
            ToolRoute.ToolOnly(IntentToolRequest.CreateReminder(topicHint = "pay rent tomorrow at 9 am")),
            result,
        )
    }
}

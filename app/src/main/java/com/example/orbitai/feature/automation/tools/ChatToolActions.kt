package com.example.orbitai.feature.automation.tools

import com.example.orbitai.feature.automation.OrbitTool
import com.example.orbitai.feature.automation.device.DeviceAction

/** A phone action the model asked for through a native tool call in chat. */
sealed interface ChatToolAction {
    /** Which Settings > Tools switch controls this action. */
    val tool: OrbitTool

    data class SendWhatsApp(val recipient: String, val message: String) : ChatToolAction { override val tool get() = OrbitTool.WHATSAPP }
    data class DraftEmail(val subject: String, val body: String) : ChatToolAction { override val tool get() = OrbitTool.EMAIL }
    /** [request] is plain text such as "call the bank tomorrow at 6 pm", read by the local reminder parser. */
    data class SetReminder(val request: String) : ChatToolAction { override val tool get() = OrbitTool.REMINDERS }
    data class Call(val contact: String) : ChatToolAction { override val tool get() = OrbitTool.CALLS }
    data class Remember(val fact: String) : ChatToolAction { override val tool get() = OrbitTool.MEMORY }
    /** Timers, alarms, SMS, navigation, apps, search, calendar and flashlight; confirmed before running. */
    data class Device(val action: DeviceAction) : ChatToolAction { override val tool get() = action.tool }
}

/** Maps tool calls from the model to [ChatToolAction]s through the [ToolRegistry]. */
object ChatToolActions {

    /** Added to the system instruction when the chat tools are offered. */
    const val INSTRUCTION = """You can act on the user's phone with tools. Call a tool only when the user asks you to do that action: send a WhatsApp or SMS message, draft an email, set a reminder, timer or alarm, call someone, remember a fact, get directions, open an app, search the web, add a calendar event, or turn the flashlight on or off. Spelling mistakes are common; understand what the user means. For everything else, answer normally without tools. Call at most one tool, and never show tool names or arguments to the user."""

    fun from(name: String, arguments: Map<String, Any?>, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): ChatToolAction? =
        ToolRegistry.toAction(name, arguments, now)
}

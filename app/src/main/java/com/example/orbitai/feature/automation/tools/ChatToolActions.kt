package com.example.orbitai.feature.automation.tools

import com.example.orbitai.feature.automation.OrbitTool
import com.example.orbitai.feature.automation.device.DeviceAction
import com.example.orbitai.feature.automation.device.DeviceActionParser
import com.example.orbitai.feature.automation.parser.LocalReminderParser

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

/** Maps tool calls from [OrbitChatToolSchema] to [ChatToolAction]s. */
object ChatToolActions {

    /** Added to the system instruction when the chat tools are offered. */
    const val INSTRUCTION = """You can act on the user's phone with tools. Call a tool only when the user asks you to do that action: send a WhatsApp or SMS message, draft an email, set a reminder, timer or alarm, call someone, remember a fact, get directions, open an app, search the web, add a calendar event, or turn the flashlight on or off. Spelling mistakes are common; understand what the user means. For everything else, answer normally without tools. Call at most one tool, and never show tool names or arguments to the user."""

    fun from(name: String, arguments: Map<String, Any?>, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): ChatToolAction? {
        fun arg(key: String) = arguments[key]?.toString()?.trim().orEmpty()
        return when (name) {
            "send_whatsapp_message" -> arg("message").takeIf { it.isNotEmpty() }
                ?.let { ChatToolAction.SendWhatsApp(arg("recipient"), it) }
            "draft_email" -> if (arg("subject").isEmpty() && arg("body").isEmpty()) null
                else ChatToolAction.DraftEmail(arg("subject"), arg("body"))
            "set_reminder" -> listOf(arg("task"), arg("time")).filter { it.isNotEmpty() }.joinToString(" ")
                .takeIf { arg("task").isNotEmpty() }?.let { ChatToolAction.SetReminder(it) }
            "call_contact" -> arg("name").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Call(it) }
            "save_memory" -> arg("fact").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Remember(it) }
            "set_timer" -> (arguments["minutes"]?.toString()?.toDoubleOrNull())?.let { minutes ->
                val seconds = (minutes * 60).toInt()
                if (seconds in 1..(24 * 3600)) ChatToolAction.Device(DeviceAction.SetTimer(seconds, arg("label").ifEmpty { null })) else null
            }
            "set_alarm" -> (DeviceActionParser.parse("/alarm ${arg("time")}", now) as? DeviceAction.SetAlarm)
                ?.let { ChatToolAction.Device(it.copy(label = arg("label").ifEmpty { null })) }
            "draft_sms" -> if (arg("recipient").isEmpty() || arg("message").isEmpty()) null
                else ChatToolAction.Device(DeviceAction.SmsDraft(arg("recipient"), arg("message")))
            "navigate" -> arg("destination").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Device(DeviceAction.Navigate(it)) }
            "open_app" -> arg("app_name").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Device(DeviceAction.OpenApp(it)) }
            "web_search" -> arg("query").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Device(DeviceAction.WebSearch(it)) }
            "add_calendar_event" -> arg("title").takeIf { it.isNotEmpty() }?.let { title ->
                val start = LocalReminderParser.parse(arg("time").ifEmpty { "tomorrow at 9 am" }, now).startTimeMillis
                ChatToolAction.Device(DeviceAction.CalendarEvent(title, start, start + 3_600_000L))
            }
            "flashlight" -> when (arg("state").lowercase()) {
                "on", "true" -> ChatToolAction.Device(DeviceAction.Flashlight(true))
                "off", "false" -> ChatToolAction.Device(DeviceAction.Flashlight(false))
                else -> null
            }
            else -> null
        }
    }
}

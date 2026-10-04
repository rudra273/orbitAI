package com.example.orbitai.feature.automation.tools

import com.example.orbitai.feature.automation.OrbitTool
import com.example.orbitai.feature.automation.device.DeviceAction
import com.example.orbitai.feature.automation.device.DeviceActionParser
import com.example.orbitai.feature.automation.parser.LocalReminderParser
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.ZonedDateTime

/** One argument of a model-callable tool. [numeric] arguments are described to the model as numbers. */
data class ToolParameter(val name: String, val description: String, val numeric: Boolean = false)

/**
 * A phone action the model can ask for, defined once: what the model is told ([name], [description],
 * [parameters]), which Settings > Tools switch controls it ([tool]), and how its arguments become a
 * [ChatToolAction] ([toAction]). Calls are never run by the model library; the chat confirms and runs them.
 */
class ToolDefinition(
    val name: String,
    val tool: OrbitTool,
    val description: String,
    val parameters: List<ToolParameter>,
    val toAction: (arguments: Map<String, Any?>, now: ZonedDateTime) -> ChatToolAction?,
) {
    /** The JSON the on-device model library reads: name, description and an object schema of the parameters. */
    fun descriptionJson(): String = JsonObject().apply {
        addProperty("name", name)
        addProperty("description", description)
        add("parameters", JsonObject().apply {
            addProperty("type", "object")
            add("properties", JsonObject().apply {
                parameters.forEach { p ->
                    add(p.name, JsonObject().apply {
                        addProperty("type", if (p.numeric) "number" else "string")
                        addProperty("description", p.description)
                    })
                }
            })
            add("required", JsonArray().apply { parameters.forEach { add(it.name) } })
        })
    }.toString()

    fun asOpenApiTool(): OpenApiTool = object : OpenApiTool {
        override fun getToolDescriptionJsonString(): String = descriptionJson()
        override fun execute(paramsJsonString: String): String = """{"status":"handled_manually"}"""
    }
}

/**
 * Every tool offered to the model in chat. To add a tool:
 * 1. add an [OrbitTool] entry (its Settings > Tools switch and slash command name),
 * 2. add its slash command to `OrbitCommands` (the "/" menu and parsers),
 * 3. add one [ToolDefinition] to [all] below (schema and argument mapping),
 * 4. add the [ChatToolAction] (or [DeviceAction]) and the code that runs it after confirmation.
 */
object ToolRegistry {

    private fun p(name: String, description: String, numeric: Boolean = false) = ToolParameter(name, description, numeric)

    val all: List<ToolDefinition> = listOf(
        ToolDefinition(
            "send_whatsapp_message", OrbitTool.WHATSAPP, "Open WhatsApp with a message ready to send to a contact.",
            listOf(p("recipient", "Contact name or phone number, for example Aditya."), p("message", "The exact message to send, written as the user, for example hi.")),
        ) { a, _ -> arg(a, "message").takeIf { it.isNotEmpty() }?.let { ChatToolAction.SendWhatsApp(arg(a, "recipient"), it) } },
        ToolDefinition(
            "draft_email", OrbitTool.EMAIL, "Open the email app with a drafted email.",
            listOf(p("subject", "Email subject."), p("body", "Full email body, written as the user.")),
        ) { a, _ -> if (arg(a, "subject").isEmpty() && arg(a, "body").isEmpty()) null else ChatToolAction.DraftEmail(arg(a, "subject"), arg(a, "body")) },
        ToolDefinition(
            "set_reminder", OrbitTool.REMINDERS, "Set a reminder on the phone.",
            listOf(p("task", "What to be reminded about, for example call the bank."), p("time", "When, in the user's words, for example tomorrow at 6 pm or in 20 minutes.")),
        ) { a, _ ->
            listOf(arg(a, "task"), arg(a, "time")).filter { it.isNotEmpty() }.joinToString(" ")
                .takeIf { arg(a, "task").isNotEmpty() }?.let { ChatToolAction.SetReminder(it) }
        },
        ToolDefinition(
            "call_contact", OrbitTool.CALLS, "Phone a contact.",
            listOf(p("name", "Contact name or phone number.")),
        ) { a, _ -> arg(a, "name").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Call(it) } },
        ToolDefinition(
            "save_memory", OrbitTool.MEMORY, "Save a fact about the user to Orbit Memory.",
            listOf(p("fact", "The fact to remember, as a short sentence.")),
        ) { a, _ -> arg(a, "fact").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Remember(it) } },
        ToolDefinition(
            "set_timer", OrbitTool.TIMER, "Start a countdown timer.",
            listOf(p("minutes", "Length in minutes, for example 10 or 0.5.", numeric = true), p("label", "Optional name, for example pasta. Empty if none.")),
        ) { a, _ ->
            a["minutes"]?.toString()?.toDoubleOrNull()?.let { minutes ->
                val seconds = (minutes * 60).toInt()
                if (seconds in 1..(24 * 3600)) ChatToolAction.Device(DeviceAction.SetTimer(seconds, arg(a, "label").ifEmpty { null })) else null
            }
        },
        ToolDefinition(
            "set_alarm", OrbitTool.ALARM, "Set an alarm clock alarm.",
            listOf(p("time", "Time like 6:30 am or 18:30."), p("label", "Optional name. Empty if none.")),
        ) { a, now ->
            (DeviceActionParser.parse("/alarm ${arg(a, "time")}", now) as? DeviceAction.SetAlarm)
                ?.let { ChatToolAction.Device(it.copy(label = arg(a, "label").ifEmpty { null })) }
        },
        ToolDefinition(
            "draft_sms", OrbitTool.SMS, "Open the SMS app with a text message ready to send (not WhatsApp).",
            listOf(p("recipient", "Contact name or phone number."), p("message", "The exact message, written as the user.")),
        ) { a, _ ->
            if (arg(a, "recipient").isEmpty() || arg(a, "message").isEmpty()) null
            else ChatToolAction.Device(DeviceAction.SmsDraft(arg(a, "recipient"), arg(a, "message")))
        },
        ToolDefinition(
            "navigate", OrbitTool.NAVIGATION, "Open maps with directions to a place, or search for nearby places.",
            listOf(p("destination", "Destination or search, for example the airport or nearest pharmacy.")),
        ) { a, _ -> arg(a, "destination").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Device(DeviceAction.Navigate(it)) } },
        ToolDefinition(
            "open_app", OrbitTool.OPEN_APP, "Open an installed app by name.",
            listOf(p("app_name", "App name, for example Spotify.")),
        ) { a, _ -> arg(a, "app_name").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Device(DeviceAction.OpenApp(it)) } },
        ToolDefinition(
            "web_search", OrbitTool.WEB_SEARCH, "Search the web in the browser when the user asks to search or google something.",
            listOf(p("query", "Search query.")),
        ) { a, _ -> arg(a, "query").takeIf { it.isNotEmpty() }?.let { ChatToolAction.Device(DeviceAction.WebSearch(it)) } },
        ToolDefinition(
            "add_calendar_event", OrbitTool.CALENDAR, "Add an event to the user's calendar.",
            listOf(p("title", "Event title."), p("time", "When, in the user's words, for example tomorrow at 4 pm.")),
        ) { a, now ->
            arg(a, "title").takeIf { it.isNotEmpty() }?.let { title ->
                val start = LocalReminderParser.parse(arg(a, "time").ifEmpty { "tomorrow at 9 am" }, now).startTimeMillis
                ChatToolAction.Device(DeviceAction.CalendarEvent(title, start, start + 3_600_000L))
            }
        },
        ToolDefinition(
            "flashlight", OrbitTool.FLASHLIGHT, "Turn the phone's flashlight on or off.",
            listOf(p("state", "on or off")),
        ) { a, _ ->
            when (arg(a, "state").lowercase()) {
                "on", "true" -> ChatToolAction.Device(DeviceAction.Flashlight(true))
                "off", "false" -> ChatToolAction.Device(DeviceAction.Flashlight(false))
                else -> null
            }
        },
    )

    private val byName = all.associateBy { it.name }

    private fun arg(arguments: Map<String, Any?>, key: String) = arguments[key]?.toString()?.trim().orEmpty()

    /** The tools to offer now: only those whose Settings > Tools switch is on. */
    fun enabled(enabledTools: Set<OrbitTool>): List<ToolDefinition> = all.filter { it.tool in enabledTools }

    /** What to pass to the model library for [enabledTools]. */
    fun openApiTools(enabledTools: Set<OrbitTool>): List<OpenApiTool> = enabled(enabledTools).map { it.asOpenApiTool() }

    /** Maps a call from the model to an action; null for an unknown tool or unusable arguments. */
    fun toAction(name: String, arguments: Map<String, Any?>, now: ZonedDateTime = ZonedDateTime.now()): ChatToolAction? =
        byName[name]?.toAction?.invoke(arguments, now)
}

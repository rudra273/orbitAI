package com.example.orbitai.feature.automation.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

/**
 * Phone actions offered to LiteRT-LM models in the main chat. Parameter names are the argument
 * keys [ChatToolActions.from] reads. Calls are not run automatically;
 * the chat maps them with [ChatToolActions] and runs them itself.
 */
class OrbitChatToolSchema : ToolSet {

    @Tool(description = "Open WhatsApp with a message ready to send to a contact.")
    fun send_whatsapp_message(
        @ToolParam(description = "Contact name or phone number, for example Aditya.")
        recipient: String,
        @ToolParam(description = "The exact message to send, written as the user, for example hi.")
        message: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Open the email app with a drafted email.")
    fun draft_email(
        @ToolParam(description = "Email subject.")
        subject: String,
        @ToolParam(description = "Full email body, written as the user.")
        body: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Set a reminder on the phone.")
    fun set_reminder(
        @ToolParam(description = "What to be reminded about, for example call the bank.")
        task: String,
        @ToolParam(description = "When, in the user's words, for example tomorrow at 6 pm or in 20 minutes.")
        time: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Phone a contact.")
    fun call_contact(
        @ToolParam(description = "Contact name or phone number.")
        name: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Save a fact about the user to Orbit Memory.")
    fun save_memory(
        @ToolParam(description = "The fact to remember, as a short sentence.")
        fact: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Start a countdown timer.")
    fun set_timer(
        @ToolParam(description = "Length in minutes, for example 10 or 0.5.")
        minutes: Double,
        @ToolParam(description = "Optional name, for example pasta. Empty if none.")
        label: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Set an alarm clock alarm.")
    fun set_alarm(
        @ToolParam(description = "Time like 6:30 am or 18:30.")
        time: String,
        @ToolParam(description = "Optional name. Empty if none.")
        label: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Open the SMS app with a text message ready to send (not WhatsApp).")
    fun draft_sms(
        @ToolParam(description = "Contact name or phone number.")
        recipient: String,
        @ToolParam(description = "The exact message, written as the user.")
        message: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Open maps with directions to a place, or search for nearby places.")
    fun navigate(
        @ToolParam(description = "Destination or search, for example the airport or nearest pharmacy.")
        destination: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Open an installed app by name.")
    fun open_app(
        @ToolParam(description = "App name, for example Spotify.")
        app_name: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Search the web in the browser when the user asks to search or google something.")
    fun web_search(
        @ToolParam(description = "Search query.")
        query: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Add an event to the user's calendar.")
    fun add_calendar_event(
        @ToolParam(description = "Event title.")
        title: String,
        @ToolParam(description = "When, in the user's words, for example tomorrow at 4 pm.")
        time: String,
    ): Map<String, String> = handledManually

    @Tool(description = "Turn the phone's flashlight on or off.")
    fun flashlight(
        @ToolParam(description = "on or off")
        state: String,
    ): Map<String, String> = handledManually

    private val handledManually get() = mapOf("status" to "handled_manually")
}

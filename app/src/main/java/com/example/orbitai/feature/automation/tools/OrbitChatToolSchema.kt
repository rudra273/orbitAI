package com.example.orbitai.feature.automation.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

/**
 * Phone actions offered to LiteRT-LM models in the main chat. Calls are not run automatically;
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

    private val handledManually get() = mapOf("status" to "handled_manually")
}

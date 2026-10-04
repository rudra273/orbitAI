package com.example.orbitai.feature.automation.tools

/** A phone action the model asked for through a native tool call in chat. */
sealed interface ChatToolAction {
    data class SendWhatsApp(val recipient: String, val message: String) : ChatToolAction
    data class DraftEmail(val subject: String, val body: String) : ChatToolAction
    /** [request] is plain text such as "call the bank tomorrow at 6 pm", read by the local reminder parser. */
    data class SetReminder(val request: String) : ChatToolAction
    data class Call(val contact: String) : ChatToolAction
    data class Remember(val fact: String) : ChatToolAction
}

/** Maps tool calls from [OrbitChatToolSchema] to [ChatToolAction]s. */
object ChatToolActions {

    /** Added to the system instruction when the chat tools are offered. */
    const val INSTRUCTION = """You can act on the user's phone with tools. Call a tool only when the user asks you to do that action: send a WhatsApp message, draft an email, set a reminder, call someone, or remember a fact. Spelling mistakes are common; understand what the user means. For everything else, answer normally without tools. Call at most one tool, and never show tool names or arguments to the user."""

    fun from(name: String, arguments: Map<String, Any?>): ChatToolAction? {
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
            else -> null
        }
    }
}

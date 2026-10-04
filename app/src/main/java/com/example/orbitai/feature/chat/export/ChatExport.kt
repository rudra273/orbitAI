package com.example.orbitai.feature.chat.export

import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.example.orbitai.feature.chat.markdownToPlainText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    MARKDOWN("Markdown", "md", "text/markdown"),
    TEXT("Plain text", "txt", "text/plain"),
    PDF("PDF", "pdf", "application/pdf"),
}

/** Turns a chat into Markdown or plain text. Messages keep their own markdown, so code blocks and lists survive. */
object ChatExport {

    fun fileName(chat: Chat, format: ExportFormat): String {
        val base = displayTitle(chat).replace(Regex("""[^\p{L}\p{N} _-]+"""), "").trim().replace(Regex("\\s+"), "-").take(60)
        return "${base.ifBlank { "orbit-chat" }}.${format.extension}"
    }

    fun displayTitle(chat: Chat): String =
        chat.title.trim().takeUnless { it.isBlank() || it.equals("New Chat", ignoreCase = true) } ?: "Orbit chat"

    /** Messages worth exporting: finished and non-empty. */
    fun exportable(chat: Chat): List<Message> =
        chat.messages.filter { !it.isStreaming && (it.content.isNotBlank() || it.imageUris.isNotEmpty()) }

    fun subtitle(chat: Chat, modelName: String?, locale: Locale = Locale.getDefault()): String {
        val date = SimpleDateFormat("d MMM yyyy, HH:mm", locale).format(Date(chat.createdAt))
        return listOfNotNull("Exported from OrbitAI", date, modelName?.let { "Model: $it" }).joinToString(" · ")
    }

    fun speaker(message: Message): String = when (message.role) {
        Role.USER -> "You"
        else -> message.modeName?.takeIf { it.isNotBlank() && it != "Orbit" }?.let { "Orbit ($it)" } ?: "Orbit"
    }

    fun markdown(chat: Chat, modelName: String?, locale: Locale = Locale.getDefault()): String = buildString {
        appendLine("# ${displayTitle(chat)}")
        appendLine()
        appendLine("_${subtitle(chat, modelName, locale)}_")
        exportable(chat).forEach { message ->
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("**${speaker(message)}**")
            appendLine()
            imageNote(message)?.let { appendLine("_${it}_"); appendLine() }
            if (message.content.isNotBlank()) appendLine(message.content.trimEnd())
        }
    }.trimEnd() + "\n"

    fun plainText(chat: Chat, modelName: String?, locale: Locale = Locale.getDefault()): String = buildString {
        appendLine(displayTitle(chat))
        appendLine(subtitle(chat, modelName, locale))
        exportable(chat).forEach { message ->
            appendLine()
            appendLine("${speaker(message)}:")
            imageNote(message)?.let { appendLine(it) }
            if (message.content.isNotBlank()) appendLine(markdownToPlainText(message.content))
        }
    }.trimEnd() + "\n"

    fun imageNote(message: Message): String? = when (val n = message.imageUris.size) {
        0 -> null
        1 -> "[1 image attached]"
        else -> "[$n images attached]"
    }
}

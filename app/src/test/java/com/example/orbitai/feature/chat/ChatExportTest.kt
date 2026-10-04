package com.example.orbitai.feature.chat

import com.example.orbitai.feature.chat.export.ChatExport
import com.example.orbitai.feature.chat.export.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ChatExportTest {

    private val reply = """
        Here's how:

        1. Install it
        2. Run it

        ```kotlin
        fun main() {
            println("**not bold**")
        }
        ```
    """.trimIndent()

    private val chat = Chat(
        title = "Kotlin help",
        createdAt = 0L,
        messages = listOf(
            Message(role = Role.USER, content = "How do I *run* Kotlin?", imageUris = listOf("content://a")),
            Message(role = Role.ASSISTANT, content = reply, modeName = "Concise"),
            Message(role = Role.ASSISTANT, content = "partial", isStreaming = true),
            Message(role = Role.ASSISTANT, content = "  "),
        ),
    )

    @Test
    fun markdownKeepsCodeBlocksAndListsVerbatim() {
        val md = ChatExport.markdown(chat, "Gemma 3n", Locale.US)
        assertTrue(md.startsWith("# Kotlin help\n"))
        assertTrue(md.contains("Model: Gemma 3n"))
        assertTrue(md.contains("**You**\n\n_[1 image attached]_\n\nHow do I *run* Kotlin?"))
        assertTrue(md.contains("**Orbit (Concise)**\n\n$reply"))
        assertFalse("streaming and blank replies are skipped", md.contains("partial"))
    }

    @Test
    fun plainTextStripsMarkdownButKeepsCode() {
        val text = ChatExport.plainText(chat, null, Locale.US)
        assertTrue(text.contains("You:\n[1 image attached]\nHow do I run Kotlin?"))
        assertTrue(text.contains("1. Install it"))
        assertTrue(text.contains("println(\"**not bold**\")"))
        assertFalse(text.contains("```"))
        assertFalse(text.contains("Model:"))
    }

    @Test
    fun fileNamesAreSafe() {
        assertEquals("Kotlin-help.md", ChatExport.fileName(chat, ExportFormat.MARKDOWN))
        assertEquals("Q3-plan-v2-final.pdf", ChatExport.fileName(chat.copy(title = "Q3 plan: v2 / final?"), ExportFormat.PDF))
        assertEquals("orbit-chat.txt", ChatExport.fileName(chat.copy(title = "???"), ExportFormat.TEXT))
        assertEquals("Orbit-chat.txt", ChatExport.fileName(chat.copy(title = "New Chat"), ExportFormat.TEXT))
    }
}

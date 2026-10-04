package com.example.orbitai.feature.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Headline features of the current major version, shown once after an update and from Settings > About. */
object WhatsNew {

    /** Bump for each major version; the sheet is shown again to people who last saw an older one. */
    const val VERSION = 2

    val items = listOf(
        "Ask for things naturally" to "Say \"send hi to Aditya on WhatsApp\" or \"set a timer for 10 minutes\", even with typos. New tools: timers, alarms, texts, navigation, calendar events and more.",
        "Spaces that show their sources" to "Answers from your documents come with the pages they used. Word and EPUB files work too.",
        "Memory you control" to "Orbit suggests what to remember and you tap to save. Your Memory is used only when it is relevant.",
        "Find and organise chats" to "Search all messages, pin, rename, archive, and export a chat as PDF or text.",
        "Share to Orbit" to "Share files or images from any app, or select text and choose Orbit to proofread, rewrite, translate or summarise.",
        "Voice in the Orbit Bubble" to "Talk hands-free from the bubble and reply to follow up. Continue in app keeps the conversation.",
        "Your phone, your data" to "Backup & restore, a Privacy center with a network log, Calendar awareness you switch on, and your own model files.",
        "Make it yours" to "Themes and text size, Modes with colours and templates, and Hindi by default for Translate (any language works).",
    )
}

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's new in Orbit ${WhatsNew.VERSION}.0") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                WhatsNew.items.forEach { (title, body) ->
                    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

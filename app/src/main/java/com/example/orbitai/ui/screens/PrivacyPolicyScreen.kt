package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.orbitai.BuildConfig

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = com.example.orbitai.ui.theme.OrbitTheme.colors.background,
        topBar = {
            com.example.orbitai.ui.components.OrbitSubScreenTopBar(
                title = stringResource(R.string.orbitai_privacy_policy),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("This policy covers the Orbit AI Android app (com.orbitai) from ROSMOX. Orbit works without an Orbit account. Downloaded AI models run on your device. Optional online features and actions you hand to other apps can send information to those services.") }
            item {
                PrivacySection(
                    title = "Chats, Spaces and local storage",
                    body = "Orbit stores saved chats and replies, selected attachments, imported documents and their searchable indexes, memories, modes, reminders, model files and settings in app storage. If Memory is enabled, Orbit can extract and reuse details from your chats. Temporary chats stay in memory and are not added to saved chat history or used to create memories.\n\nYou choose which files, images, shared text or selected text to open in Orbit. Orbit can copy generated text to your Android clipboard so you can paste it. The app uses private Android storage for API credentials and excludes its app data from Android cloud backup and device transfer. The Android app has no advertising or analytics SDKs.",
                )
            }
            item {
                PrivacySection(
                    title = "Gemini and model downloads",
                    body = "If you configure Gemini with your own API key and approve a cloud request, Orbit sends Google your key and the context needed for the reply. This can include the prompt or voice transcript, conversation history, attached images, instructions, relevant memories and excerpts from documents or Spaces. Google handles the request under its Gemini API terms and privacy practices. Choose a local model to keep AI inference on your device.\n\nModel and embedding downloads contact Hugging Face or Google model hosting. Hugging Face receives your token if authentication is needed. Online providers receive network information such as your IP address when your device connects.",
                )
            }
            item {
                PrivacySection(
                    title = "Microphone, speech and screen capture",
                    body = "Microphone permission is used for voice input you start. Orbit prefers Android's on-device speech recognition when available. Otherwise, your configured speech service may send audio to its provider. Orbit does not store audio recordings. A transcript can become part of a chat if you submit it. When you use Read aloud, Orbit passes the selected text to your Android text-to-speech engine, which may use its provider's network voices.\n\nThe floating bubble needs permission to appear above other apps. Screen actions ask Android for permission to capture the screen. The bubble processes captured images temporarily with its local model and does not save them as image files. You can stop the bubble from its notification or disable it in Settings. If you choose Orbit as Android's digital assistant, Android can open Orbit from the configured assistant gesture or button.",
                )
            }
            item {
                PrivacySection(
                    title = "Message replies and notification digest",
                    body = "These features are optional and require Android notification access and your selection of supported apps. Orbit reads notification titles and message previews from those selections. It uses a local model to draft a reply or make a digest when you ask. Previews, drafts and digest inputs stay in memory and are not saved to chat history or sent to Gemini.\n\nIf you review and send a reply, Orbit hands the text to the originating messaging app for delivery. You can clear the digest, disable the features or revoke notification access in Android Settings.",
                )
            }
            item {
                PrivacySection(
                    title = "Contacts and actions in other apps",
                    body = "With contact permission, Orbit looks up contact names and numbers to help with calls, SMS and WhatsApp drafts. A direct call requires your confirmation, phone permission and supported hardware. Other actions can open a dialer, messaging app, email app, browser or search service, maps, calendar or clock and pass the number, draft, query, destination or event details needed for the action. The receiving app or service handles that information under its own privacy terms.\n\nReminders are stored on your device. Orbit uses notification permission to show alerts and optional Alarms & reminders access for precise delivery.",
                )
            }
            item {
                PrivacySection(
                    title = "Exports, backups and diagnostics",
                    body = "You can export or share content with an app or destination you select. Manual backups encrypt the database and selected settings with your passphrase and can include API credentials if you choose. Android's file picker may offer a cloud destination. That destination controls the exported copy.\n\nThe Privacy center shows up to 30 days of local network activity, including time, host, purpose and response size when known. It does not log request bodies, full URLs or credentials. Orbit may keep up to 10 local diagnostics reports for crashes, freezes and low-memory exits. Reports may include app and Android versions, device and model details, timestamps and technical traces. Orbit attempts to redact known credential patterns. Diagnostics are shared only when you choose to share them.",
                )
            }
            item {
                PrivacySection(
                    title = "AI response reports",
                    body = "When you confirm an AI response report, Orbit sends ROSMOX the displayed reply or excerpt, your optional note, the app version, its source and a random report ID. Rosmox.com forwards these details through its transactional email provider, Brevo, to the ROSMOX moderation inbox. Orbit keeps a daily submission count on your device. The website may use your network address to limit abuse.\n\nROSMOX uses reports for moderation and keeps report emails in the restricted moderation inbox for up to 30 days. Review the text before sending, since it can contain personal information. To request earlier deletion, email admin@rosmox.com with your report ID.",
                )
            }
            item {
                PrivacySection(
                    title = "Retention, deletion and contact",
                    body = "Saved app content stays on your device until you remove it. Use Settings and the Privacy center to manage chats, memories, Spaces, cached data, models and network history. Clear the app's storage to remove its local app data. Deleting a Space removes Orbit's imported data and index. You can remove API credentials in Settings and clear diagnostics in the diagnostics screen.\n\nCopies you export or share, original files and data previously sent to another service are managed separately by you or the receiving provider. Contact ROSMOX for questions about this policy or requests about information you sent to ROSMOX.",
                )
            }
            item {
                PrivacySection("Privacy contact", BuildConfig.SUPPORT_EMAIL.ifBlank { "admin@rosmox.com" })
                if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                    TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                        Text(stringResource(R.string.read_the_public_privacy_policy))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

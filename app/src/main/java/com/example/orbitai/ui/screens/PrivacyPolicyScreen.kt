package com.example.orbitai.ui.screens

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
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back") }
                Text("OrbitAI Privacy policy", style = MaterialTheme.typography.headlineMedium)
            }
            item { PrivacySection("Data on your device", "Orbit stores chats, imported document text, memories, reminders, model files, settings and API keys in app storage on your device. Local AI models process your prompts on the device. Orbit does not include advertising or analytics SDKs. Android cloud backup and device transfer of Orbit's app data are disabled.") }
            item { PrivacySection("Optional Gemini cloud AI", "Before sending a Gemini request, Orbit asks for your confirmation. The request may contain your message, chat history, attached images and document text, relevant memories, and excerpts from selected Spaces. Google receives these data and your API key to generate a response under its Gemini API terms and privacy practices. Choose a local model to use on-device inference.") }
            item { PrivacySection("Microphone and screen capture", "Microphone access is used when you activate voice input. Orbit prefers on-device speech recognition when available; your device's speech service may send audio to its provider otherwise. Orbit does not save audio recordings. Screen capture requires Android's approval for each capture and is processed by the bubble's local model. Screenshots are used temporarily and are not saved as image files. The floating bubble needs permission to display over other apps. You can stop it from its notification or disable it in Settings.") }
            item { PrivacySection("Contacts and external apps", "Contacts access is used to find phone numbers for calls and WhatsApp drafts by contact name. Phone permission is used only after you confirm a number and tap Call; you can open the dialer instead. Orbit opens the draft in the external app for you to review and send. Orbit stores reminder schedules locally. Reminder notifications require notification permission, and precise delivery uses the optional Alarms & reminders access. Documents and images are selected using Android's file picker.") }
            item { PrivacySection("Optional notification replies", "If you enable Message replies and grant Android notification access, Orbit receives notifications and keeps reply-capable message previews only from your selected apps. These previews and reply drafts stay in memory and are not written to chat history or sent to cloud AI. AI drafting uses a downloaded local model. Tapping Send reply hands the text to the originating messaging app, which handles delivery under its own policies. Disable Message replies to clear the previews, or revoke notification access in Android Settings.") }
            item { PrivacySection("Network requests and reports", "Model downloads contact Hugging Face or Google model hosting; Hugging Face receives your token when an authenticated download needs it. Update checks contact GitHub. These services receive normal connection information, such as your IP address. When you submit an AI-content report, the selected response, optional note, app version and source (chat or bubble) are sent over HTTPS to the developer for moderation. Review the selected response for personal information before submitting.") }
            item { PrivacySection("Retention and deletion", "Local chats, documents and memories remain until you delete them or clear the app's storage. Deleting a Space removes its imported documents and indexed chunks from Orbit; original files remain in their original location. API keys and model downloads can be removed in Settings. Clearing app storage removes local data. Previously submitted data may remain with external service providers according to their policies. For report retention periods and requests to delete submitted reports, see the public policy and privacy contact below.") }
            item {
                if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                    PrivacySection("Privacy contact", BuildConfig.SUPPORT_EMAIL)
                }
                if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                    TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                        Text("Read the public privacy policy")
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

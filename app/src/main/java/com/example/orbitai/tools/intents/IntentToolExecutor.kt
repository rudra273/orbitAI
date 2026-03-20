package com.example.orbitai.tools.intents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IntentToolExecutor(context: Context) {

    private val appContext = context.applicationContext
    private val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
    private val contactResolver = ContactResolver(appContext)

    suspend fun execute(request: IntentToolRequest, draft: EmailDraft): IntentToolExecutionResult {
        return when (request) {
            is IntentToolRequest.DraftEmail -> launchDraftEmail(draft)
            is IntentToolRequest.DraftWhatsApp -> IntentToolExecutionResult.Failed("Unsupported draft type for WhatsApp request.")
        }
    }

    suspend fun execute(request: IntentToolRequest, draft: WhatsAppDraft): IntentToolExecutionResult {
        return when (request) {
            is IntentToolRequest.DraftWhatsApp -> launchDraftWhatsApp(draft)
            is IntentToolRequest.DraftEmail -> IntentToolExecutionResult.Failed("Unsupported draft type for email request.")
        }
    }

    private suspend fun launchDraftEmail(draft: EmailDraft): IntentToolExecutionResult = withContext(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, draft.subject)
            putExtra(Intent.EXTRA_TEXT, draft.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            appContext.startActivity(intent)
            IntentToolExecutionResult.Launched
        } catch (_: ActivityNotFoundException) {
            IntentToolExecutionResult.Failed("No email app found on this device.")
        }
    }

    private suspend fun launchDraftWhatsApp(draft: WhatsAppDraft): IntentToolExecutionResult = withContext(Dispatchers.Main) {
        val recipientName = draft.recipientName.trim()
        val baseIntent = if (recipientName.isBlank()) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, draft.message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
                return@withContext IntentToolExecutionResult.PermissionRequired(
                    permission = RuntimeToolPermission.CONTACTS,
                    message = "Grant contacts permission to send WhatsApp messages by contact name.",
                )
            }

            val phoneNumber = contactResolver.findPhoneNumberByName(recipientName)
                ?: return@withContext IntentToolExecutionResult.Failed(
                    "Couldn't find a contact named $recipientName.",
                )

            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://wa.me/${Uri.encode(phoneNumber)}?text=${Uri.encode(draft.message)}"
                ),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        for (packageName in whatsAppPackages) {
            val intent = Intent(baseIntent).apply {
                `package` = packageName
            }
            try {
                appContext.startActivity(intent)
                return@withContext IntentToolExecutionResult.Launched
            } catch (_: ActivityNotFoundException) {
            }
        }

        IntentToolExecutionResult.Failed("WhatsApp is not installed on this device.")
    }
}

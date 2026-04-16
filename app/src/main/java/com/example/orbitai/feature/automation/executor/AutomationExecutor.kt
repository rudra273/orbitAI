package com.example.orbitai.feature.automation.executor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.parser.EmailDraft
import com.example.orbitai.feature.automation.parser.ReminderDraft
import com.example.orbitai.feature.automation.parser.RuntimeToolPermission
import com.example.orbitai.feature.automation.parser.WhatsAppDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutomationExecutor(context: Context) {

    private val appContext = context.applicationContext
    private val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
    private val contactResolver = ContactResolver(appContext)

    suspend fun execute(request: AutomationRequest, draft: EmailDraft): AutomationExecutionResult {
        return when (request) {
            is AutomationRequest.DraftEmail -> launchDraftEmail(draft)
            is AutomationRequest.DraftWhatsApp -> AutomationExecutionResult.Failed("Unsupported draft type for WhatsApp request.")
            is AutomationRequest.CreateReminder -> AutomationExecutionResult.Failed("Unsupported draft type for reminder request.")
        }
    }

    suspend fun execute(request: AutomationRequest, draft: WhatsAppDraft): AutomationExecutionResult {
        return when (request) {
            is AutomationRequest.DraftWhatsApp -> launchDraftWhatsApp(draft)
            is AutomationRequest.DraftEmail -> AutomationExecutionResult.Failed("Unsupported draft type for email request.")
            is AutomationRequest.CreateReminder -> AutomationExecutionResult.Failed("Unsupported draft type for reminder request.")
        }
    }

    suspend fun execute(request: AutomationRequest, draft: ReminderDraft): AutomationExecutionResult {
        return when (request) {
            is AutomationRequest.CreateReminder -> launchReminder(draft)
            is AutomationRequest.DraftEmail -> AutomationExecutionResult.Failed("Unsupported draft type for email request.")
            is AutomationRequest.DraftWhatsApp -> AutomationExecutionResult.Failed("Unsupported draft type for WhatsApp request.")
        }
    }

    private suspend fun launchDraftEmail(draft: EmailDraft): AutomationExecutionResult = withContext(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, draft.subject)
            putExtra(Intent.EXTRA_TEXT, draft.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            appContext.startActivity(intent)
            AutomationExecutionResult.Launched
        } catch (_: ActivityNotFoundException) {
            AutomationExecutionResult.Failed("No email app found on this device.")
        }
    }

    private suspend fun launchDraftWhatsApp(draft: WhatsAppDraft): AutomationExecutionResult = withContext(Dispatchers.Main) {
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
                return@withContext AutomationExecutionResult.PermissionRequired(
                    permission = RuntimeToolPermission.CONTACTS,
                    message = "Grant contacts permission to send WhatsApp messages by contact name.",
                )
            }

            val phoneNumber = contactResolver.findPhoneNumberByName(recipientName)
                ?: return@withContext AutomationExecutionResult.Failed(
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
                return@withContext AutomationExecutionResult.Launched
            } catch (_: ActivityNotFoundException) {
            }
        }

        AutomationExecutionResult.Failed("WhatsApp is not installed on this device.")
    }

    private suspend fun launchReminder(draft: ReminderDraft): AutomationExecutionResult = withContext(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, draft.title)
            putExtra(CalendarContract.Events.DESCRIPTION, draft.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, draft.startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, draft.endTimeMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            appContext.startActivity(intent)
            AutomationExecutionResult.Launched
        } catch (_: ActivityNotFoundException) {
            AutomationExecutionResult.Failed("No calendar app found on this device.")
        }
    }
}

package com.example.orbitai.tools.intents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IntentToolExecutor(context: Context) {

    private val appContext = context.applicationContext

    suspend fun execute(request: IntentToolRequest, draft: EmailDraft): IntentToolExecutionResult {
        return when (request) {
            is IntentToolRequest.DraftEmail -> launchDraftEmail(draft)
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
}

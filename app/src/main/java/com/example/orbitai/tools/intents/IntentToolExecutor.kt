package com.example.orbitai.tools.intents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.orbitai.data.Message

class IntentToolExecutor(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    fun execute(request: IntentToolRequest, messages: List<Message>): IntentToolExecutionResult {
        return when (request) {
            is IntentToolRequest.DraftEmail -> launchDraftEmail(messages, request.topicHint)
        }
    }

    private fun launchDraftEmail(
        messages: List<Message>,
        topicHint: String,
    ): IntentToolExecutionResult {
        val draft = EmailDraftBuilder.build(messages = messages, topicHint = topicHint)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, draft.subject)
            putExtra(Intent.EXTRA_TEXT, draft.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(appContext.packageManager) == null) {
            return IntentToolExecutionResult.Failed("No email app found on this device.")
        }

        mainHandler.post {
            appContext.startActivity(intent)
        }
        return IntentToolExecutionResult.Launched
    }
}

package com.example.orbitai.feature.automation.replies

import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A preview of one currently active notification, never written to disk. */
data class ReplyNotification(
    val key: String,
    val revision: Long,
    val packageName: String,
    val conversation: String,
    val message: String,
    val postedAt: Long,
)

internal data class ReplyTarget(
    val preview: ReplyNotification,
    val action: Notification.Action,
    val input: RemoteInput,
)

internal object ReplyTargetParser {
    fun parse(sbn: StatusBarNotification, revision: Long): ReplyTarget? {
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val action = notification.actions.orEmpty().filter { action ->
            (action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY ||
                notification.category == Notification.CATEGORY_MESSAGE) &&
                action.remoteInputs.orEmpty().count { it.allowFreeFormInput } == 1 &&
                action.actionIntent?.let {
                    !it.isImmutable && !it.isActivity && it.creatorPackage == sbn.packageName
                } == true
        }.singleOrNull() ?: return null
        val title = notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        val message = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        if (title.isNullOrBlank() || message.isNullOrBlank()) return null
        return ReplyTarget(
            ReplyNotification(sbn.key, revision, sbn.packageName, title.toString().take(300), message.toString().take(4000), sbn.postTime),
            action,
            action.remoteInputs.single { it.allowFreeFormInput },
        )
    }
}

class NotificationReplyListener : NotificationListenerService() {
    private val targets = mutableMapOf<String, ReplyTarget>()
    private var revision = 0L

    override fun onListenerConnected() {
        connected = this
        refresh()
    }

    override fun onListenerDisconnected() {
        clear()
        if (connected === this) connected = null
    }

    override fun onDestroy() {
        onListenerDisconnected()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        targets.remove(sbn.key)
        if (NotificationReplySettings(this).allows(sbn.packageName)) {
            ReplyTargetParser.parse(sbn, ++revision)?.let { targets[sbn.key] = it }
        }
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        targets.remove(sbn.key)
        publish()
    }

    private fun clear() {
        targets.clear()
        previews.value = emptyList()
    }

    private fun publish() {
        // Bound memory even if an app posts many conversations.
        val newest = targets.values.sortedByDescending { it.preview.postedAt }.take(50)
        targets.keys.retainAll(newest.map { it.preview.key }.toSet())
        previews.value = newest.map { it.preview }
    }

    private fun refresh() {
        clear()
        if (!NotificationReplySettings(this).enabled) return
        try {
            activeNotifications.orEmpty().forEach(::onNotificationPosted)
        } catch (_: SecurityException) {
            clear()
        }
    }

    private fun send(preview: ReplyNotification, text: String): String? {
        if (text.isBlank() || text.length > 4000) return "Enter a reply of 1–4000 characters."
        if (!NotificationReplySettings(this).allows(preview.packageName)) return "Replies for this app are disabled."
        if (getSystemService(KeyguardManager::class.java).isDeviceLocked) return "Unlock your device before replying."
        val target = targets[preview.key]
        if (target?.preview != preview) return "This notification changed. Open the latest message before replying."
        try {
            val active = getActiveNotifications(arrayOf(preview.key)).orEmpty().singleOrNull()
                ?: return "This notification is no longer available."
            val current = ReplyTargetParser.parse(active, preview.revision)
                ?: return "This notification no longer supports replies."
            if (current.preview != preview || current.action.actionIntent != target.action.actionIntent ||
                current.input.resultKey != target.input.resultKey) return "This message changed. Review it again."
            val result = Intent()
            RemoteInput.addResultsToIntent(arrayOf(current.input), result, Bundle().apply {
                putCharSequence(current.input.resultKey, text.trim())
            })
            RemoteInput.setResultsSource(result, RemoteInput.SOURCE_FREE_FORM_INPUT)
            current.action.actionIntent.send(this, 0, result)
            targets.remove(preview.key)
            publish()
            return null
        } catch (_: PendingIntent.CanceledException) {
            targets.remove(preview.key)
            publish()
            return "The reply action expired. Open the messaging app to reply."
        } catch (_: SecurityException) {
            return "Android blocked this reply. Open the messaging app to reply."
        }
    }

    companion object {
        private var connected: NotificationReplyListener? = null
        private val previews = MutableStateFlow<List<ReplyNotification>>(emptyList())
        val notifications = previews.asStateFlow()

        // Called from the UI thread, like NotificationListenerService callbacks.
        fun refreshSettings() {
            previews.value = emptyList()
            connected?.refresh()
        }

        fun sendReply(preview: ReplyNotification, text: String): String? {
            val service = connected ?: return "Notification access is disconnected. Re-enable access in Android Settings."
            return service.send(preview, text)
        }
    }
}

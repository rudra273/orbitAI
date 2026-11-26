package com.example.orbitai.replies

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbitai.feature.automation.reminder.ReminderReceiver
import com.example.orbitai.feature.automation.replies.NotificationReplySettings
import com.example.orbitai.feature.automation.replies.ReplyTargetParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import java.util.UUID

class ReplyTargetParserTest {
    private val pendingIntents = mutableListOf<PendingIntent>()
    @After fun cleanUp() { pendingIntents.forEach { it.cancel() } }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun action(immutable: Boolean = false, freeForm: Boolean = true): Notification.Action {
        val pending = PendingIntent.getBroadcast(context, 0,
            Intent(context, ReminderReceiver::class.java).setAction(UUID.randomUUID().toString()),
            if (immutable) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_MUTABLE)
        pendingIntents += pending
        return Notification.Action.Builder(null, "Reply", pending)
            .setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY)
            .addRemoteInput(RemoteInput.Builder("reply").setAllowFreeFormInput(freeForm).build()).build()
    }

    private fun posted(actions: List<Notification.Action>, summary: Boolean = false,
                       packageName: String = context.packageName, text: String = "Can we meet tomorrow?"): StatusBarNotification {
        val builder = Notification.Builder(context, "test")
            .setContentTitle("Alex").setContentText(text).setCategory(Notification.CATEGORY_MESSAGE)
        actions.forEach(builder::addAction)
        if (summary) builder.setGroup("messages").setGroupSummary(true)
        return StatusBarNotification(packageName, packageName, 1, null, Process.myUid(), 0, 0,
            builder.build(), Process.myUserHandle(), 100L)
    }

    @Test fun acceptsOneMutableReplyAndPreservesConversation() {
        val parsed = ReplyTargetParser.parse(posted(listOf(action())), 7)!!
        assertEquals("Alex", parsed.preview.conversation)
        assertEquals("Can we meet tomorrow?", parsed.preview.message)
        assertEquals(7L, parsed.preview.revision)
        assertEquals("reply", parsed.input.resultKey)
    }

    @Test fun rejectsImmutableAndChoiceOnlyActions() {
        assertNull(ReplyTargetParser.parse(posted(listOf(action(immutable = true))), 1))
        assertNull(ReplyTargetParser.parse(posted(listOf(action(freeForm = false))), 1))
    }

    @Test fun rejectsGroupSummaryAndAmbiguousTargets() {
        assertNull(ReplyTargetParser.parse(posted(listOf(action()), summary = true), 1))
        assertNull(ReplyTargetParser.parse(posted(listOf(action(), action())), 1))
    }

    @Test fun rejectsMissingContentAndActionsOwnedByAnotherPackage() {
        assertNull(ReplyTargetParser.parse(posted(listOf(action()), text = ""), 1))
        assertNull(ReplyTargetParser.parse(posted(listOf(action()), packageName = "another.app"), 1))
    }

    @Test fun accessIsOffByDefaultAndLimitedToSelectedApps() {
        val name = "reply-settings-test-${UUID.randomUUID()}"
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences =
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
        try {
            val settings = NotificationReplySettings(isolated)
            assertFalse(settings.allows("com.whatsapp"))
            settings.packages = setOf("com.whatsapp")
            assertFalse(settings.allows("com.whatsapp"))
            settings.enabled = true
            assertTrue(settings.allows("com.whatsapp"))
            assertFalse(settings.allows("org.telegram.messenger"))
            settings.enabled = false
            assertFalse(settings.allows("com.whatsapp"))
        } finally { context.deleteSharedPreferences(name) }
    }
}

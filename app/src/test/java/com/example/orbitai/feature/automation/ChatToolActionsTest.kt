package com.example.orbitai.feature.automation

import com.example.orbitai.feature.automation.tools.ChatToolAction
import com.example.orbitai.feature.automation.tools.ChatToolActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatToolActionsTest {

    @Test
    fun mapsEachTool() {
        assertEquals(
            ChatToolAction.SendWhatsApp("Aditya", "hi"),
            ChatToolActions.from("send_whatsapp_message", mapOf("recipient" to " Aditya ", "message" to "hi")),
        )
        assertEquals(
            ChatToolAction.DraftEmail("Q3 numbers", "Hi Priya, ..."),
            ChatToolActions.from("draft_email", mapOf("subject" to "Q3 numbers", "body" to "Hi Priya, ...")),
        )
        assertEquals(
            ChatToolAction.SetReminder("call the bank tomorrow at 6 pm"),
            ChatToolActions.from("set_reminder", mapOf("task" to "call the bank", "time" to "tomorrow at 6 pm")),
        )
        assertEquals(ChatToolAction.Call("Mom"), ChatToolActions.from("call_contact", mapOf("name" to "Mom")))
        assertEquals(ChatToolAction.Remember("bike on level 2"), ChatToolActions.from("save_memory", mapOf("fact" to "bike on level 2")))
    }

    @Test
    fun rejectsUnknownOrEmptyCalls() {
        assertNull(ChatToolActions.from("delete_everything", emptyMap()))
        assertNull(ChatToolActions.from("send_whatsapp_message", mapOf("recipient" to "Aditya", "message" to " ")))
        assertNull(ChatToolActions.from("set_reminder", mapOf("time" to "6 pm")))
        assertNull(ChatToolActions.from("call_contact", mapOf("name" to null)))
        assertNull(ChatToolActions.from("draft_email", emptyMap()))
    }
}

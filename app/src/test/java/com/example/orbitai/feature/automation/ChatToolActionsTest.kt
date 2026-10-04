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

    @Test
    fun mapsDeviceTools() {
        val now = java.time.ZonedDateTime.parse("2026-10-03T18:15:00+05:30[Asia/Kolkata]")
        fun device(name: String, vararg args: Pair<String, Any?>) =
            (ChatToolActions.from(name, args.toMap(), now) as ChatToolAction.Device).action
        assertEquals(com.example.orbitai.feature.automation.device.DeviceAction.SetTimer(600, null), device("set_timer", "minutes" to 10.0, "label" to ""))
        assertEquals(com.example.orbitai.feature.automation.device.DeviceAction.SetAlarm(6, 30, "gym"), device("set_alarm", "time" to "6:30 am", "label" to "gym"))
        assertEquals(com.example.orbitai.feature.automation.device.DeviceAction.Flashlight(true), device("flashlight", "state" to "on"))
        assertEquals(com.example.orbitai.feature.automation.device.DeviceAction.OpenApp("Spotify"), device("open_app", "app_name" to "Spotify"))
        val event = device("add_calendar_event", "title" to "Dentist", "time" to "tomorrow at 4 pm") as com.example.orbitai.feature.automation.device.DeviceAction.CalendarEvent
        assertEquals("2026-10-04T16:00+05:30[Asia/Kolkata]", java.time.Instant.ofEpochMilli(event.startMillis).atZone(now.zone).toString())
        assertEquals(com.example.orbitai.feature.automation.OrbitTool.ALARM, ChatToolActions.from("set_alarm", mapOf("time" to "7 am"), now)!!.tool)
        assertNull(ChatToolActions.from("set_timer", mapOf("minutes" to 0), now))
        assertNull(ChatToolActions.from("flashlight", mapOf("state" to "dim"), now))
    }
}

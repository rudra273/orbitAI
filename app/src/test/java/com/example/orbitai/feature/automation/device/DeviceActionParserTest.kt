package com.example.orbitai.feature.automation.device

import com.example.orbitai.feature.automation.device.DeviceAction.CalendarEvent
import com.example.orbitai.feature.automation.device.DeviceAction.Flashlight
import com.example.orbitai.feature.automation.device.DeviceAction.Navigate
import com.example.orbitai.feature.automation.device.DeviceAction.OpenApp
import com.example.orbitai.feature.automation.device.DeviceAction.SetAlarm
import com.example.orbitai.feature.automation.device.DeviceAction.SetTimer
import com.example.orbitai.feature.automation.device.DeviceAction.SmsDraft
import com.example.orbitai.feature.automation.device.DeviceAction.WebSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime

class DeviceActionParserTest {

    private val now = ZonedDateTime.parse("2026-10-03T18:15:00+05:30[Asia/Kolkata]")
    private fun parse(text: String) = DeviceActionParser.parse(text, now)

    @Test
    fun timers() {
        assertEquals(SetTimer(600, null), parse("set a timer for 10 minutes"))
        assertEquals(SetTimer(600, null), parse("timer 10 min"))
        assertEquals(SetTimer(5400, null), parse("set a timer for 1 hour and 30 minutes"))
        assertEquals(SetTimer(300, "pasta"), parse("start a 5 minute timer for pasta"))
        assertEquals(SetTimer(45, null), parse("/timer 45s"))
        assertEquals(SetTimer(600, null), parse("10 minute ka timer laga do"))
        assertEquals(SetTimer(600, null), parse("Hey Orbit, can you set a timer for 10 minutes"))
        assertNull(parse("how does a kitchen timer work?"))
    }

    @Test
    fun alarms() {
        assertEquals(SetAlarm(6, 0, null), parse("set an alarm for 6 am"))
        assertEquals(SetAlarm(6, 30, null), parse("wake me up at 6:30 am"))
        assertEquals(SetAlarm(19, 45, "gym"), parse("alarm at 7:45 pm for gym"))
        assertEquals(SetAlarm(5, 0, null), parse("/alarm 5"))
        assertEquals(SetAlarm(6, 0, null), parse("subah 6 baje ka alarm laga do"))
        assertEquals(SetAlarm(21, 0, null), parse("raat 9 baje alarm"))
        assertNull(parse("set an alarm for 13 pm"))
    }

    @Test
    fun smsDrafts() {
        assertEquals(SmsDraft("Rahul", "I'm running late"), parse("send a text to Rahul saying I'm running late"))
        assertEquals(SmsDraft("mom", "reached home"), parse("text mom that reached home"))
        assertEquals(SmsDraft("9876543210", "call me"), parse("sms 9876543210: call me"))
        assertNull(parse("text mom on whatsapp that I reached"))
    }

    @Test
    fun navigation() {
        assertEquals(Navigate("the airport"), parse("navigate to the airport"))
        assertEquals(Navigate("MG Road"), parse("take me to MG Road"))
        assertEquals(Navigate("Puri"), parse("how do I get to Puri?"))
        assertEquals(Navigate("nearest petrol pump"), parse("find the nearest petrol pump"))
        assertEquals(Navigate("station"), parse("station ka rasta batao"))
    }

    @Test
    fun openAppsSearchAndFlashlight() {
        assertEquals(OpenApp("Spotify"), parse("open Spotify"))
        assertEquals(OpenApp("google maps"), parse("launch the google maps app"))
        assertEquals(OpenApp("camera"), parse("camera kholo"))
        assertNull(parse("open the door for the guests waiting outside"))
        assertEquals(WebSearch("best biryani in Hyderabad"), parse("search for best biryani in Hyderabad"))
        assertEquals(WebSearch("ipl score"), parse("google ipl score"))
        assertNull(parse("search my documents for the invoice"))
        assertEquals(Flashlight(true), parse("turn on the flashlight"))
        assertEquals(Flashlight(false), parse("torch off"))
        assertEquals(Flashlight(true), parse("torch jala do"))
        assertEquals(Flashlight(false), parse("flashlight band karo"))
    }

    @Test
    fun calendarEvents() {
        val event = parse("add an event dentist tomorrow at 4 pm") as CalendarEvent
        assertEquals("dentist", event.title)
        assertEquals("2026-10-04T16:00+05:30[Asia/Kolkata]", Instant.ofEpochMilli(event.startMillis).atZone(now.zone).toString())
        assertEquals(3_600_000L, event.endMillis - event.startMillis)
        assertTrue(parse("schedule a meeting called sprint review on monday at 10 am") is CalendarEvent)
    }

    @Test
    fun ordinaryChatIsNotAnAction() {
        listOf(
            "what is a timer interrupt?",
            "tell me a story about an alarm clock",
            "explain how GPS navigation works",
            "I love texting my friends",
            "write a poem",
        ).forEach { assertNull(it, parse(it)) }
    }

    @Test
    fun confirmationsAreReadable() {
        assertEquals("Set a timer for 1 hour 30 minutes?", SetTimer(5400, null).confirmation())
        assertEquals("Set an alarm for 6:30 AM (gym)?", SetAlarm(6, 30, "gym").confirmation())
        assertEquals("Set an alarm for 12:00 PM?", SetAlarm(12, 0, null).confirmation())
    }

    @Test
    fun appMatching() {
        val labels = listOf("Maps", "Google Maps Go", "Spotify", "Camera", "Calculator", "WhatsApp Business", "WhatsApp")
        assertEquals("Spotify", labels[AppMatcher.best("spotify", labels)!!])
        assertEquals("Maps", labels[AppMatcher.best("maps", labels)!!])
        assertEquals("WhatsApp", labels[AppMatcher.best("whatsapp", labels)!!])
        assertEquals("Calculator", labels[AppMatcher.best("calculater", labels)!!])
        assertNull(AppMatcher.best("netflix", labels))
    }
}

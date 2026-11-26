package com.example.orbitai.feature.automation.device

import com.example.orbitai.feature.automation.OrbitTool
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A phone action handed to another app through a standard Android intent, after the user confirms. */
sealed interface DeviceAction {
    val tool: OrbitTool

    data class SetTimer(val seconds: Int, val label: String?) : DeviceAction { override val tool get() = OrbitTool.TIMER }
    data class SetAlarm(val hour: Int, val minute: Int, val label: String?) : DeviceAction { override val tool get() = OrbitTool.ALARM }
    data class SmsDraft(val recipient: String, val message: String) : DeviceAction { override val tool get() = OrbitTool.SMS }
    data class Navigate(val destination: String) : DeviceAction { override val tool get() = OrbitTool.NAVIGATION }
    data class OpenApp(val appName: String) : DeviceAction { override val tool get() = OrbitTool.OPEN_APP }
    data class WebSearch(val query: String) : DeviceAction { override val tool get() = OrbitTool.WEB_SEARCH }
    data class CalendarEvent(val title: String, val startMillis: Long, val endMillis: Long) : DeviceAction { override val tool get() = OrbitTool.CALENDAR }
    data class Flashlight(val on: Boolean) : DeviceAction { override val tool get() = OrbitTool.FLASHLIGHT }

    /** Question for the confirmation card, e.g. "Set a timer for 10 minutes?". */
    fun confirmation(zone: ZoneId = ZoneId.systemDefault()): String = when (this) {
        is SetTimer -> "Set a timer for ${duration(seconds)}" + (label?.let { " ($it)" } ?: "") + "?"
        is SetAlarm -> "Set an alarm for ${clock(hour, minute)}" + (label?.let { " ($it)" } ?: "") + "?"
        is SmsDraft -> "Open your SMS app with a message to $recipient?\n\n“$message”"
        is Navigate -> "Get directions to $destination?"
        is OpenApp -> "Open $appName?"
        is WebSearch -> "Search the web for “$query”?"
        is CalendarEvent -> "Add “$title” to your calendar on " +
            Instant.ofEpochMilli(startMillis).atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM, h:mm a")) + "?"
        is Flashlight -> if (on) "Turn the flashlight on?" else "Turn the flashlight off?"
    }

    companion object {
        fun duration(seconds: Int): String {
            val h = seconds / 3600
            val m = seconds % 3600 / 60
            val s = seconds % 60
            return listOfNotNull(
                h.takeIf { it > 0 }?.let { "$it hour" + if (it > 1) "s" else "" },
                m.takeIf { it > 0 }?.let { "$it minute" + if (it > 1) "s" else "" },
                s.takeIf { it > 0 }?.let { "$it second" + if (it > 1) "s" else "" },
            ).joinToString(" ").ifEmpty { "0 seconds" }
        }

        fun clock(hour: Int, minute: Int): String {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            return "$h12:${minute.toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
        }
    }
}

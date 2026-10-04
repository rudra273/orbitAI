package com.example.orbitai.feature.automation.device

import com.example.orbitai.feature.automation.OrbitCommands
import com.example.orbitai.feature.automation.parser.CommandSpelling
import com.example.orbitai.feature.automation.parser.DeviceCommandParser
import com.example.orbitai.feature.automation.parser.LocalReminderParser
import java.time.ZonedDateTime

/**
 * Deterministic phrasings for timers, alarms, SMS drafts, navigation, opening apps, web search,
 * calendar events and the flashlight, in English and common Hinglish. Anything else is left to
 * the model (which can call the same actions as native tools).
 */
object DeviceActionParser {

    private const val TIME = """(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?"""

    fun parse(input: String, now: ZonedDateTime = ZonedDateTime.now()): DeviceAction? {
        val text = DeviceCommandParser.normalize(input).replace(Regex("""(?i)^please\s+"""), "").trim().trimEnd('.', '!')
        if (text.isBlank() || text.endsWith("?") && !text.contains(Regex("(?i)\\b(?:can|could|will|would) you\\b"))) {
            // Questions are answered by the model; "how do I get to X?" is the one exception.
            return navigate(text)
        }
        return timer(text) ?: flashlight(text) ?: alarm(text) ?: sms(text) ?: navigate(text) ?: calendar(text, now) ?: webSearch(text) ?: openApp(text)
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    private val durationPart = Regex("""(?i)(\d{1,3})\s*(h|hrs?|hours?|ghante?|m|mins?|minutes?|s|secs?|seconds?)\b""")

    private fun timer(text: String): DeviceAction? {
        val slash = Regex("""(?i)^/(?:${OrbitCommands.slashNames("timer")})\s+(.+)$""").find(text)?.groupValues?.get(1)
        val english = Regex("""(?i)^(?:set|start|put|create|run)?\s*(?:an?\s+)?timer\s+(?:for\s+)?(.+)$""").find(text)?.groupValues?.get(1)
        val leading = Regex("""(?i)^(?:set|start)?\s*(?:an?\s+)?(.+?)\s+timer(?:\s+for\s+(.+))?$""").find(text)
        val hinglish = Regex("""(?i)^(.+?)\s+(?:ka|ki)\s+timer(?:\s+(?:laga|lagao|laga\s+do|set\s+karo?|set\s+kar\s+do|chalu\s+karo?))?$""").find(text)
        val (spec, label) = when {
            slash != null -> slash to null
            english != null -> english to null
            hinglish != null -> hinglish.groupValues[1] to null
            leading != null -> leading.groupValues[1] to leading.groupValues[2].ifBlank { null }
            else -> return null
        }
        val parts = durationPart.findAll(spec).toList()
        if (parts.isEmpty()) return null
        val seconds = parts.sumOf { part ->
            val n = part.groupValues[1].toInt()
            when (part.groupValues[2].lowercase().first()) {
                'h', 'g' -> n * 3600
                's' -> n
                else -> n * 60
            }
        }.takeIf { it in 1..(24 * 3600) } ?: return null
        val rest = spec.replace(durationPart, " ").replace(Regex("""(?i)\b(?:and|for|ka|ki)\b"""), " ").replace(Regex("\\s+"), " ").trim()
        return DeviceAction.SetTimer(seconds, (label ?: rest).trim().takeIf { it.isNotEmpty() })
    }

    // ── Alarm ────────────────────────────────────────────────────────────────

    private fun alarm(text: String): DeviceAction? {
        val patterns = listOf(
            Regex("""(?i)^/(?:${OrbitCommands.slashNames("alarm")})\s+$TIME(?:\s+(.+))?$"""),
            Regex("""(?i)^(?:set|put|create)?\s*(?:an?\s+)?alarm\s+(?:for\s+|at\s+)?$TIME(?:\s+(?:for|to)\s+(.+))?$"""),
            Regex("""(?i)^wake\s+me(?:\s+up)?\s+at\s+$TIME(?:\s+(?:for|to)\s+(.+))?$"""),
            // Hinglish: "6 baje ka alarm laga do", "subah 6:30 baje alarm"
            Regex("""(?i)^(?:subah\s+|sham\s+|raat\s+)?$TIME\s*baje\s+(?:ka\s+)?alarm(?:\s+(?:laga|lagao|laga\s+do|set\s+karo?|set\s+kar\s+do))?()$"""),
        )
        for (pattern in patterns) {
            val m = pattern.find(text) ?: continue
            var hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
            val period = m.groupValues[3].lowercase().replace(".", "")
            if (minute !in 0..59) return null
            if (period.isNotEmpty()) {
                if (hour !in 1..12) return null
                hour = hour % 12 + if (period == "pm") 12 else 0
            } else if (text.contains(Regex("(?i)\\b(?:sham|raat)\\b")) && hour in 1..11) {
                hour += 12
            }
            if (hour !in 0..23) return null
            return DeviceAction.SetAlarm(hour, minute, m.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() })
        }
        return null
    }

    // ── SMS ──────────────────────────────────────────────────────────────────

    private fun sms(text: String): DeviceAction? {
        if (Regex("(?i)\\bwhats\\s*app\\b").containsMatchIn(text) ||
            Regex("""[A-Za-z']+""").findAll(text).any { CommandSpelling.matches(it.value, "whatsapp") }) return null
        val patterns = listOf(
            Regex("""(?is)^/(?:${OrbitCommands.slashNames("sms")})\s+(\S+)\s+(.+)$"""),
            Regex("""(?is)^(?:send|draft|write)\s+(?:an?\s+)?(?:sms|text(?:\s+message)?|message)\s+to\s+(.+?)\s*(?:saying|that says|that|:|,)\s*(.+)$"""),
            Regex("""(?is)^(?:sms|text)\s+(.+?)\s*(?:saying|that|:|,)\s*(.+)$"""),
        )
        for (pattern in patterns) {
            val m = pattern.find(text) ?: continue
            val recipient = m.groupValues[1].trim()
            val message = m.groupValues[2].trim()
            if (recipient.isNotEmpty() && message.isNotEmpty() && recipient.split(" ").size <= 4) {
                return DeviceAction.SmsDraft(recipient, message)
            }
        }
        return null
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private fun navigate(text: String): DeviceAction? {
        val patterns = listOf(
            Regex("""(?i)^/(?:${OrbitCommands.slashNames("navigate")})\s+(.+)$"""),
            Regex("""(?i)^(?:navigate|directions|route|drive|take\s+me)\s+(?:me\s+)?(?:to\s+)(.+)$"""),
            Regex("""(?i)^(?:get\s+)?directions\s+(?:to\s+)?(.+)$"""),
            Regex("""(?i)^how\s+(?:do|can)\s+i\s+(?:get|go)\s+to\s+(.+?)\??$"""),
            Regex("""(?i)^(?:find|show)\s+(?:me\s+)?(?:the\s+)?(nearest\s+.+|.+\s+near\s+me)$"""),
            Regex("""(?i)^where\s+is\s+the\s+(nearest\s+.+?)\??$"""),
            Regex("""(?i)^(.+?)\s+(?:ka|ke|ki)\s+rasta(?:\s+(?:batao|dikhao))?$"""),
        )
        for (pattern in patterns) {
            val destination = pattern.find(text)?.groupValues?.get(1)?.trim()?.trimEnd('?', '.')
            if (!destination.isNullOrBlank()) return DeviceAction.Navigate(destination)
        }
        return null
    }

    // ── Calendar ─────────────────────────────────────────────────────────────

    private fun calendar(text: String, now: ZonedDateTime): DeviceAction? {
        val m = Regex(
            """(?i)^(?:/(?:${OrbitCommands.slashNames("event")})|(?:add|create|schedule|put|make)\s+(?:an?\s+)?(?:calendar\s+)?(?:event|meeting|appointment))\s*(?:to\s+(?:my\s+)?calendar\s*)?(?:called|named|for|:)?\s*(.+)$""",
        ).find(text) ?: return null
        val draft = LocalReminderParser.parse(m.groupValues[1], now)
        if (draft.title.isBlank()) return null
        return DeviceAction.CalendarEvent(draft.title, draft.startTimeMillis, draft.startTimeMillis + 60 * 60_000L)
    }

    // ── Web search ───────────────────────────────────────────────────────────

    private fun webSearch(text: String): DeviceAction? {
        if (Regex("(?i)\\b(?:my|these)\\s+(?:documents?|notes|files|spaces?|chats?)\\b").containsMatchIn(text)) return null
        val m = Regex("""(?i)^(?:/(?:${OrbitCommands.slashNames("search")})|search(?:\s+the\s+web|\s+online|\s+google)?|google|look\s+up)\s+(?:for\s+)?(.+)$""").find(text)
            ?: Regex("""(?i)^(.+?)\s+(?:google|search)\s+karo?$""").find(text)
            ?: return null
        return m.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { DeviceAction.WebSearch(it) }
    }

    // ── Open app ─────────────────────────────────────────────────────────────

    private fun openApp(text: String): DeviceAction? {
        val m = Regex("""(?i)^(?:/(?:${OrbitCommands.slashNames("open")})|open|launch)\s+(?:the\s+)?(.+?)(?:\s+app)?$""").find(text)
            ?: Regex("""(?i)^(.+?)\s+(?:kholo|khol\s+do|open\s+karo?)$""").find(text)
            ?: return null
        val name = m.groupValues[1].trim()
        // "open the door" style chat stays chat: app names are short.
        return name.takeIf { it.isNotEmpty() && it.split(" ").size <= 3 }?.let { DeviceAction.OpenApp(it) }
    }

    // ── Flashlight ───────────────────────────────────────────────────────────

    private fun flashlight(text: String): DeviceAction? {
        val light = "(?:flash\\s*light|torch)"
        Regex("""(?i)^(?:/(?:${OrbitCommands.slashNames("torch")})\s+(on|off)|(?:turn|switch)\s+(on|off)\s+(?:the\s+)?$light|(?:turn|switch)\s+(?:the\s+)?$light\s+(on|off)|$light\s+(on|off))$""")
            .find(text)?.let { m ->
                return DeviceAction.Flashlight(m.groupValues.drop(1).first { it.isNotEmpty() }.equals("on", true))
            }
        Regex("""(?i)^$light\s+(jala\s*do|jalao|on\s+karo?|chalu\s+karo?|band\s+karo?|bujha\s*do|off\s+karo?)$""").find(text)?.let { m ->
            val v = m.groupValues[1].lowercase()
            return DeviceAction.Flashlight(!(v.startsWith("band") || v.startsWith("bujha") || v.startsWith("off")))
        }
        return null
    }
}

package com.example.orbitai.feature.automation

/** Every phone action Orbit can take, each with an on/off switch in Settings > Tools. */
enum class OrbitTool(val title: String, val description: String, val command: String?) {
    EMAIL("Email drafts", "Writes a draft and opens your email app.", "mail"),
    WHATSAPP("WhatsApp messages", "Drafts a message and opens WhatsApp; you press Send.", "whatsapp"),
    REMINDERS("Reminders", "Saves Orbit reminders, including repeating ones.", "remind"),
    CALLS("Calls", "Finds the contact and opens the phone app after you confirm.", "call"),
    MEMORY("Remember", "Saves facts you ask Orbit to remember.", "remember"),
    TIMER("Timers", "Starts a timer in your clock app.", "timer"),
    ALARM("Alarms", "Sets an alarm in your clock app.", "alarm"),
    SMS("Text messages", "Opens your SMS app with the message ready; you press Send.", "sms"),
    NAVIGATION("Navigation", "Opens maps with directions or a place search.", "navigate"),
    OPEN_APP("Open apps", "Opens an installed app by name.", "open"),
    WEB_SEARCH("Web search", "Searches the web in your browser.", "search"),
    CALENDAR("Calendar events", "Opens your calendar with a new event filled in.", "event"),
    FLASHLIGHT("Flashlight", "Turns the flashlight on or off.", "torch"),
}

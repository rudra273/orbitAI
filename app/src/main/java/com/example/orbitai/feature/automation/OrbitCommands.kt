package com.example.orbitai.feature.automation

/** A built-in slash command. [aliases] are extra slash names the parsers accept. */
data class OrbitCommand(
    val name: String,
    val aliases: List<String>,
    val title: String,
    val description: String,
    val example: String,
)

/** Single source for the slash commands shown in the "/" menu and accepted by the parsers. */
object OrbitCommands {

    val all: List<OrbitCommand> = listOf(
        OrbitCommand("mail", listOf("email", "draft-mail", "draft-email"), "Draft an email",
            "Writes a draft and opens your mail app", "/mail ask Priya for the Q3 numbers"),
        OrbitCommand("whatsapp", listOf("wa"), "WhatsApp message",
            "Drafts a message and opens WhatsApp", "/whatsapp tell Mom I'll be late"),
        OrbitCommand("remind", listOf("reminder"), "Set a reminder",
            "Saves an Orbit reminder", "/remind at 6 pm to call the bank"),
        OrbitCommand("call", emptyList(), "Call someone",
            "Finds the contact and opens the phone app", "/call Mom"),
        OrbitCommand("remember", emptyList(), "Remember something",
            "Saves a fact to Orbit Memory", "/remember my bike is on level 2"),
        OrbitCommand("timer", emptyList(), "Start a timer", "Starts a timer in your clock app", "/timer 10m"),
        OrbitCommand("alarm", emptyList(), "Set an alarm", "Sets an alarm in your clock app", "/alarm 6:30 am"),
        OrbitCommand("sms", listOf("text"), "Text message", "Opens your SMS app with the message ready", "/sms Rahul running late"),
        OrbitCommand("navigate", listOf("directions"), "Directions", "Opens maps with directions", "/navigate the airport"),
        OrbitCommand("open", emptyList(), "Open an app", "Opens an installed app", "/open Spotify"),
        OrbitCommand("search", emptyList(), "Search the web", "Searches in your browser", "/search monsoon forecast"),
        OrbitCommand("event", emptyList(), "Calendar event", "Opens your calendar with the event filled in", "/event dentist tomorrow at 4 pm"),
        OrbitCommand("torch", listOf("flashlight"), "Flashlight", "Turns the flashlight on or off", "/torch on"),
    )

    fun byName(name: String): OrbitCommand = all.first { it.name == name }

    /** Regex alternation of a command's slash names, e.g. `mail|email|draft\-mail|draft\-email`. */
    fun slashNames(name: String): String =
        byName(name).let { listOf(it.name) + it.aliases }.joinToString("|") { Regex.escape(it) }
}

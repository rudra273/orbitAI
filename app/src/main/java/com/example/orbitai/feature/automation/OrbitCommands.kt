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
    )

    fun byName(name: String): OrbitCommand = all.first { it.name == name }

    /** Regex alternation of a command's slash names, e.g. `mail|email|draft\-mail|draft\-email`. */
    fun slashNames(name: String): String =
        byName(name).let { listOf(it.name) + it.aliases }.joinToString("|") { Regex.escape(it) }
}

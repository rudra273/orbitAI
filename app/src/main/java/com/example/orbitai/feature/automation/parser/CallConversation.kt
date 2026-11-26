package com.example.orbitai.feature.automation.parser

import com.example.orbitai.feature.automation.executor.CallContact
import com.example.orbitai.feature.automation.executor.ContactNameMatcher

sealed interface CallReply {
    data object Confirm : CallReply
    data object Cancel : CallReply
    data object OpenDialer : CallReply
    data class Select(val contact: CallContact) : CallReply
    data object Repeat : CallReply
}

object CallConversation {
    fun question(contacts: List<CallContact>, selected: CallContact?): String {
        if (selected != null) return "Do you want to call ${label(selected, contacts)}? Say yes to call, or no to cancel."
        val options = contacts.mapIndexed { index, contact -> "${index + 1}, ${label(contact, contacts)}" }
        return "I found several close matches: ${options.joinToString("; ")}. Say the name or option number."
    }

    fun reply(text: String, contacts: List<CallContact>, selected: CallContact?): CallReply {
        val answer = ContactNameMatcher.normalize(DeviceCommandParser.normalize(text))
        val selection = selected?.takeIf { it in contacts }
        if (answer in setOf("no", "no thanks", "no thank you", "cancel", "cancel call", "never mind", "nevermind", "stop", "don t call")) return CallReply.Cancel
        if (answer in setOf("yes", "yes please", "yeah", "yep", "sure", "okay", "ok", "correct", "that s right", "go ahead", "yes call", "yes call them", "call them")) {
            return if (selection != null) CallReply.Confirm else CallReply.Repeat
        }
        if (answer in setOf("open dialer", "open the dialer", "use the dialer")) {
            return if (selection != null) CallReply.OpenDialer else CallReply.Repeat
        }
        val ordinals = listOf(setOf("1", "one", "first", "the first one", "option one", "option 1"),
            setOf("2", "two", "second", "the second one", "option two", "option 2"),
            setOf("3", "three", "third", "the third one", "option three", "option 3"))
        val index = answer.removePrefix("option ").toIntOrNull()?.minus(1)
            ?: ordinals.indexOfFirst { answer in it }
        if (index >= 0) return contacts.getOrNull(index)?.let(CallReply::Select) ?: CallReply.Repeat
        val matches = ContactNameMatcher.closest(answer, contacts)
        return matches.singleOrNull()?.let(CallReply::Select) ?: CallReply.Repeat
    }

    private fun label(contact: CallContact, contacts: List<CallContact>): String {
        val duplicates = contacts.count { ContactNameMatcher.normalize(it.name) == ContactNameMatcher.normalize(contact.name) }
        val name = contact.name.ifBlank { contact.number }
        return if (duplicates > 1) "$name, number ending in ${contact.number.takeLast(4).toList().joinToString(" ")}" else name
    }
}

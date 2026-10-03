package com.example.orbitai.feature.automation.parser

sealed interface DeviceCommand {
    data class Call(val recipient: String) : DeviceCommand
    data class Remember(val content: String) : DeviceCommand
}

object DeviceCommandParser {
    fun normalize(input: String): String = input.trim()
        .replace(Regex("(?i)^(?:hey\\s+)?orbit[,!:]?\\s+"), "")
        .replace(Regex("(?i)^(?:(?:can|could|would) you\\s+)?(?:please\\s+)?"), "")
        .trim()

    fun parse(input: String): DeviceCommand? {
        val text = normalize(input)
        Regex("(?is)^(?:(?:i (?:want|need|would like) to )?(?:/call|call|phone|dial)|(?:make|place) a call to)\\s+(.+)$").matchEntire(text)?.let {
            val recipient = it.groupValues[1].trim().replace(Regex("(?i)[, ]+please[.!?]*$"), "").trimEnd('.', '!', '?').trim()
            if (recipient.isNotBlank()) return DeviceCommand.Call(recipient)
        }
        Regex("(?is)^(?:/remember|remember(?:\\s+(?:that|this))?|save (?:this )?(?:to|in) memory)\\s*[:,-]?\\s+(.+)$")
            .matchEntire(text)?.let {
                if (Regex("(?i)^me\\b").containsMatchIn(it.groupValues[1])) return null
                return DeviceCommand.Remember(it.groupValues[1].trim())
            }
        return null
    }
}

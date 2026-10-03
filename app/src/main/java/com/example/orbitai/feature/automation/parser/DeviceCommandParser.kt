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
        Regex("(?is)^(?:/call|call|phone|dial)\\s+(.+)$").matchEntire(text)?.let {
            return DeviceCommand.Call(it.groupValues[1].trim().removeSuffix(" please").trim())
        }
        Regex("(?is)^(?:/remember|remember(?:\\s+(?:that|this))?|save (?:this )?(?:to|in) memory)\\s*[:,-]?\\s+(.+)$")
            .matchEntire(text)?.let {
                if (Regex("(?i)^me\\b").containsMatchIn(it.groupValues[1])) return null
                return DeviceCommand.Remember(it.groupValues[1].trim())
            }
        return null
    }
}

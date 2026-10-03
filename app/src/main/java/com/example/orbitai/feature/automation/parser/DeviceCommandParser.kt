package com.example.orbitai.feature.automation.parser

import com.example.orbitai.feature.automation.OrbitCommands

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
        Regex("(?is)^(?:(?:i (?:want|need|would like) to )?(?:/(?:${OrbitCommands.slashNames("call")})|call|phone|dial)|(?:make|place) a call to)\\s+(.+)$").matchEntire(text)?.let {
            val recipient = it.groupValues[1].trim().replace(Regex("(?i)[, ]+please[.!?]*$"), "").trimEnd('.', '!', '?').trim()
            if (recipient.isNotBlank()) return DeviceCommand.Call(recipient)
        }
        Regex("(?is)^(?:/(?:${OrbitCommands.slashNames("remember")})|remember(?:\\s+(?:that|this))?|save (?:this )?(?:to|in) memory)\\s*[:,-]?\\s+(.+)$")
            .matchEntire(text)?.let {
                if (Regex("(?i)^me\\b").containsMatchIn(it.groupValues[1])) return null
                return DeviceCommand.Remember(it.groupValues[1].trim())
            }
        return null
    }
}
